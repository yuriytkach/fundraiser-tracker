package com.yuriytkach.tracker.fundraiser.service;

import com.yuriytkach.tracker.fundraiser.AwsLambdaIntegrationTestCommon;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TrackServiceIT extends AwsLambdaIntegrationTestCommon {

    @Test
    public void testProcessTrackCommand() {
        AwsProxyRequest request = createAwsProxyRequest(
            "/slack/cmd",
            "POST",
            Map.of(
                "Content-Type", MediaType.APPLICATION_FORM_URLENCODED,
                "Accept", MediaType.APPLICATION_JSON
            )
        );

        String response = RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .when()
            .post(LAMBDA_URL_PATH)
            .then()
            .statusCode(200)
            .extract()
            .asString();

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.contains("Tracked"));
    }
}
