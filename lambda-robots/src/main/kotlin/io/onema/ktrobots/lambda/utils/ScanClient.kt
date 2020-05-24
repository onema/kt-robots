package io.onema.ktrobots.lambda.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.onema.ktrobots.commons.domain.ScanEnemiesRequest
import io.onema.ktrobots.commons.domain.ScanEnemiesResponse
import kotlinx.coroutines.runBlocking

class ScanClient(private val apiUrl: String, private val gameId: String, private val robotId: String, private val httpClient: HttpClient, private val mapper: ObjectMapper) {
    fun scan(heading: Double, resolution: Double): ScanEnemiesResponse =
        runBlocking {
            val requestBody = ScanEnemiesRequest(
                gameId,
                robotId,
                heading,
                resolution
            )
            val response =
                httpClient.post<ScanEnemiesResponse>("http://$apiUrl/scan") {
                    contentType(ContentType.Application.Json)
                    body = requestBody
                }
            response
        }
}