/**
 * This file is part of the ONEMA RobotServer Package.
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed
 * with this source code.
 *
 * copyright (c) 2020, Juan Manuel Torres (http://onema.io)
 *
 * @author Juan Manuel Torres <software@onema.io>
 */

package io.onema.ktrobots.commons.domain

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped

enum class GameStatus {
    undefined,
    start,
    nextTurn,
    finished,
    error
}

interface Locatable {
    var id: String
    var x: Double
    var y: Double
}

@DynamoDBDocument
data class Game(

    var id: String = "",

    // current state
    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
    var status: GameStatus = GameStatus.undefined,
    var missiles: List<LambdaRobotMissile> = listOf(),
    var robots: List<LambdaRobot> = listOf(),
    var messages: List<Message> = listOf(),

    // game characteristics
    var info: GameInfo = GameInfo(),

    var robotTimeoutSeconds: Double = 0.0,
    var minRobotStartDistance: Double = 0.0

) {

    fun aliveCount(): Int = robots.count {it.status == LambdaRobotStatus.alive }

    fun updateRobot(newRobot: LambdaRobot): Game {
        // Remove the old robot and update it with the new one
        val updatedRobots = robots.filter {it.id != newRobot.id} + newRobot

        // pass an updated copy of the game to the next step
        return this.copy(robots = updatedRobots)
    }

    fun updateMissile(newMissile: LambdaRobotMissile): Game {
        // Remove the old missile and update it with the new one
        val updatedMissiles = missiles.filter {it.id != newMissile.id} + newMissile

        // pass an updated copy of the game to the next step
        return this.copy(missiles = updatedMissiles)
    }

    fun cleanupMissiles(): Game = copy(missiles = missiles.filter { it.status != MissileStatus.destroyed })
}

@DynamoDBDocument
data class GameInfo(
    /**
     * Width of the game board.
     */
    var boardWidth : Double = 1000.0,

    /**
     * Height of the game board.
     */
    var boardHeight : Double = 1000.0,

    /**
     * Number of seconds elapsed per game turn.
     */
    var secondsPerTurn : Double = 0.5,

    /**
     * Distance for missile impact to count as direct hit.
     */
    var directHitRange: Double = 5.0,

    /**
     * Distance for missile impact to count as near hit.
     */
    var nearHitRange: Double = 20.0,

    /**
     * Distance for missile impact to count as far hit.
     */
    var farHitRange: Double = 40.0,

    /**
     * Distance between robots to count as a collision.
     */
    var collisionRange: Double = 8.0,

    /**
     * Current game turn. Starts at `1`.
     */
    var gameTurn : Int = 0,

    /**
     * Maximum number of turns before the game ends in a draw.
     */
    var maxGameTurns : Int = 300,

    /**
     * Maximum number of build points a robot can use.
     */
    var maxBuildPoints : Int = 8,

    /**
     * The URL of the game server
     */
    var apiUrl: String = ""
)

enum class MissileStatus{
    undefined,
    flying,
    explodingDirect,
    explodingNear,
    explodingFar,
    destroyed;

    companion object {
        fun explodingStatus(): List<MissileStatus> = listOf(explodingFar, explodingNear, explodingDirect)
    }
}

@DynamoDBDocument
data class LambdaRobotMissile(
    override var id: String = "",
    var robotId: String = "",

    // current state
    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
    var status: MissileStatus = MissileStatus.undefined,
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    var distance: Double = 0.0,

    // missile characteristics
    var speed: Double = 0.0,
    var heading: Double = 0.0,
    var range: Double = 0.0,
    var directHitDamageBonus: Double = 0.0,
    var nearHitDamageBonus: Double = 0.0,
    var farHitDamageBonus: Double = 0.0
) : Locatable {
    fun doMove(moveData: MoveData): LambdaRobotMissile = copy(x = moveData.x, y = moveData.y, distance = moveData.distance)

    fun updateExplodeStatus(): LambdaRobotMissile {
        return when(status) {
            MissileStatus.flying -> this
            MissileStatus.explodingDirect -> copy(status = MissileStatus.explodingNear)
            MissileStatus.explodingNear -> copy(status = MissileStatus.explodingFar)
            MissileStatus.explodingFar -> copy(status = MissileStatus.destroyed)
            else -> copy(status = MissileStatus.destroyed)
        }
    }
}

@DynamoDBDocument
data class Message(
    var gameTurn: Int = 0,
    var text: String = ""
)


data class MoveData(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val distance: Double = 0.0,
    val collision: Boolean = false
)
