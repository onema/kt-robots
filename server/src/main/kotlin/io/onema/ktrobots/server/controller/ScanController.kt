/**
 * This file is part of the ONEMA ktrobots Package.
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed
 * with this source code.
 *
 * copyright (c) 2020, Juan Manuel Torres (http://onema.io)
 *
 * @author Juan Manuel Torres <software@onema.io>
 */

package io.onema.ktrobots.server.controller

import io.onema.ktrobots.commons.domain.ScanEnemiesRequest
import io.onema.ktrobots.commons.domain.ScanEnemiesResponse
import io.onema.ktrobots.commons.utils.angleToXY
import io.onema.ktrobots.commons.utils.distance
import io.onema.ktrobots.server.data.GameTableRepository
import io.onema.ktrobots.server.domain.GameRecord
import io.onema.ktrobots.server.service.GameLogic
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.RuntimeException
import kotlin.math.PI
import kotlin.math.atan2

@RestController
@RequestMapping("/scan")
class ScanController(val repo: GameTableRepository) {

    @PostMapping
    fun scan(@RequestBody request: ScanEnemiesRequest): ScanEnemiesResponse {
        val gameRecord: GameRecord = repo.findByPrimaryKey(request.gameId).orElseThrow{ RuntimeException("Game ${request.gameId} not found") }
        val robot = gameRecord.game.robots.first { it.id == request.robotId }
        val robotOption = GameLogic.scanRobots(gameRecord.game, robot, request.heading, request.resolution)
        return if (robotOption.isPresent) {
            val other = robotOption.get()
            val distance = distance(robot.x, robot.y, other.x, other.y)
            val heading = angleToXY(other.x, other.y, robot)//
            ScanEnemiesResponse(found = true, distance = distance, heading = heading)
        } else {
            ScanEnemiesResponse()
        }
    }
}
