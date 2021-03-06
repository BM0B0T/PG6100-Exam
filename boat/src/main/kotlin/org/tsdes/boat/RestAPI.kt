package org.tsdes.boat

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.tsdes.advanced.rest.dto.PageDto
import org.tsdes.advanced.rest.dto.RestResponseFactory
import org.tsdes.advanced.rest.dto.WrappedResponse
import org.tsdes.boat.db.BoatService
import org.tsdes.boat.db.toDto
import org.tsdes.dto.BoatDto
import java.net.URI
import java.util.concurrent.TimeUnit

@Api(value = "api/boat", description = "Endpoint for managing Trips")
@RestController
@RequestMapping(
    path = ["api/boat"],
    produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
class RestAPI(
    private val boatService: BoatService,
    private val rabbit: RabbitTemplate,
    private val topicExchange: TopicExchange
) {

    @GetMapping("/{boatId}")
    fun getBoat(
        @PathVariable("boatId") id: String
    ): ResponseEntity<WrappedResponse<BoatDto>> {
        val boat = boatService.getById(id.toLong()) ?: return RestResponseFactory.notFound("Boat with $id not found")
        return RestResponseFactory.payload(200, boat.toDto())
    }

    @PostMapping
    @ApiOperation("Create a new Boat")
    fun createBoat(
        @ApiParam("Dto of New Boat")
        @RequestBody dto: BoatDto
    ): ResponseEntity<WrappedResponse<Void>> {
        val boat =
            boatService.registerNewBoat(dto.name, dto.builder, dto.numberOfCrew, dto.maxPassengers, dto.minPassengers)
        rabbit.convertAndSend(topicExchange.name, "create", boat.id)
        return RestResponseFactory.created(URI.create("api/port/${boat.id}"))
    }

    @PutMapping("/{boatId}")
    @ApiOperation("update a Boat")
    fun updateTrip(
        @ApiParam("Id of New Boat")
        @PathVariable("boatId") id: Long,
        @ApiParam("Dto of New Boat")
        @RequestBody dto: BoatDto
    ): ResponseEntity<WrappedResponse<Void>> {
        if (id != dto.id) {
            return RestResponseFactory.userFailure("id in path and in body dont match, $id")
        }
        if (!boatService.updateBoat(dto)) {
            return RestResponseFactory.userFailure("boat with id = $id dose not exist")
        }
        rabbit.convertAndSend(topicExchange.name, "update", id)
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Return an iterable page of ports")
    @GetMapping
    fun getAllTrips(
        @RequestParam("keysetId", required = false)
        keysetId: Long?
    ): ResponseEntity<WrappedResponse<PageDto<BoatDto>>> {
        // Set amount if not supplied
        val amount = 10
        val page = PageDto<BoatDto>().apply {
            list = (boatService.getNextPage(amount, keysetId).map { it.toDto() })
        }
        if (page.list.size == amount)
            page.next = "/api/boat?keysetId=${page.list.last().id}"
        return ResponseEntity
            .status(200)
            .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic())
            .body(WrappedResponse(200, page).validated())
    }
}