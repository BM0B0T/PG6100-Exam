package org.tsdes.boat

import io.swagger.annotations.ApiModelProperty

class BoatDto(
    @get:ApiModelProperty("The ID of the Boat")
    var id: Long? = null,
    @get:ApiModelProperty("The ID of the Boat")
    var name: String = "",
    @get:ApiModelProperty("The ID of the Boat")
    var builder: String = "",
    @get:ApiModelProperty("The ID of the Boat")
    var numberOfCrew: Int = 0,
)
