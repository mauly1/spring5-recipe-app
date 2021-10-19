package guru.springframework.Test;

public class testForSomething {
}
/*

package com.telstra.outageservice.controllers

import com.netflix.hystrix.exception.HystrixRuntimeException
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.telstra.outageservice.adapters.Clock
import com.telstra.outageservice.adapters.DateConverter
import com.telstra.outageservice.adapters.outage.Cause
import com.telstra.outageservice.adapters.outage.FallbackServiceOutageClientException
import com.telstra.outageservice.adapters.outage.TechnologyType
import com.telstra.outageservice.domain.*
import com.telstra.outageservice.domain.Point
import com.telstra.outageservice.domain.Polygon
import com.telstra.outageservice.domain.gateways.RecaptchaTokenVerificationGateway
import com.telstra.outageservice.domain.gateways.AddressVerificationGateway
import com.telstra.outageservice.domain.gateways.ServiceOutageGateway
import com.telstra.outageservice.exception.ResponseEntityExceptionHandler
import io.restassured.module.mockmvc.RestAssuredMockMvc
import io.restassured.module.mockmvc.RestAssuredMockMvc.`when`
import io.restassured.module.mockmvc.RestAssuredMockMvc.given
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@RunWith(SpringRunner::class)
class ServiceOutageControllerTest {
    private val clock: Clock = mock()
    private val serviceOutageGateway: ServiceOutageGateway = mock()
    private val addressVerificationGateway: AddressVerificationGateway = mock()
    private val recaptchaTokenVerificationGateway: RecaptchaTokenVerificationGateway = mock()
    private val validAddress = ValidAddress(adborId = "adbor-id", locality = "Sydney", streetNumber = "123", street = "My St", state = "MyState")
    private val fnn = "0297251195"
    private val idType = "FNN"
    private val serviceType = "ADSL"
    private lateinit var mockMvc: MockMvc

    @Before
    fun setup() {
        val controller = ServiceOutageController(serviceOutageGateway, addressVerificationGateway, true)
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(ResponseEntityExceptionHandler()).build()
        RestAssuredMockMvc.mockMvc(mockMvc)
        whenever(serviceOutageGateway.getFixedOutage(validAddress)).thenReturn(ServiceOutageData(false,listOf(),""))
        whenever(serviceOutageGateway.getMobileOutage("HACKETT")).thenReturn(listOf())
        whenever(recaptchaTokenVerificationGateway.isValid("token")).thenReturn(true)
        whenever(serviceOutageGateway.getMassOutages(validAddress.state)).thenReturn(listOf())
        whenever(serviceOutageGateway.getFixedOutageByServiceIdForAdsl("xyz@bigpond.com")).thenReturn(ServiceOutageData(false,listOf(),""))
    }

    @Test
    fun getServiceOutages_whenAddressHasOutage_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)
        whenever(serviceOutageGateway
                .getFixedOutage(validAddress))
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.ADSL.name,
                        description = "ADSL is down",
                        impacted_services = "Data",
                        cause = Cause.POWER,
                        end = now, technology = "ADSL",
                        product_type = "ABCD"
                )
                ),""))

        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("POWER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("ADSL is down"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("ABCD"))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway).getFixedOutage(validAddress)
    }

    @Test
    fun getServiceOutages_whenAddressHasMassStateOutage_returnsOutage() {
        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)

         whenever(serviceOutageGateway.getMassOutages(validAddress.state))
                .thenReturn(listOf(MassOutage("NSW Outage", description = "There is an outage in NSW")))

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(0))
                .andExpect(jsonPath("massStateOutage.title").value("NSW Outage"))
                .andExpect(jsonPath("massStateOutage.description").value("There is an outage in NSW"))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway).getMassOutages(validAddress.state)
    }

    @Test
    fun getServiceOutages_whenMassStateOutageIsFalsy_returnsOutage() {

        val serviceOutagecontroller = ServiceOutageController(serviceOutageGateway, addressVerificationGateway, false)
        this.mockMvc = MockMvcBuilders.standaloneSetup(serviceOutagecontroller).setControllerAdvice(ResponseEntityExceptionHandler()).build()
        RestAssuredMockMvc.mockMvc(mockMvc)

        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)

        whenever(serviceOutageGateway.getMassOutages(validAddress.state))
                .thenReturn(listOf(MassOutage("NSW Outage", description = "There is an outage in NSW")))

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(0))
                .andExpect(jsonPath("massStateOutage.title").value(""))
                .andExpect(jsonPath("massStateOutage.description").value(""))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway, never()).getMassOutages(validAddress.state)
    }

    @Test
    fun getServiceOutages_whenAddressHasNoMassStateOutage_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)

        whenever(serviceOutageGateway.getMassOutages(validAddress.state))
                .thenReturn(listOf())

        whenever(serviceOutageGateway
                .getFixedOutage(validAddress))
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.ADSL.name,
                        description = "ADSL is down",
                        impacted_services = "Data",
                        cause = Cause.POWER,
                        end = now, technology = "ADSL",
                        product_type = "No Worries"
                )
                ),""))


        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("POWER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("ADSL is down"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("No Worries"))
                .andExpect(jsonPath("massStateOutage.title").value(""))
                .andExpect(jsonPath("massStateOutage.description").value(""))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway).getMassOutages(validAddress.state)
        verify(serviceOutageGateway).getFixedOutage(validAddress)
    }

    @Test
    fun getServiceOutages_whenMassStateOutageReturnsException_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        val fallbackException = HystrixRuntimeException(null, null, null, null, Exception(Exception(FallbackServiceOutageClientException())))

        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)

        whenever(serviceOutageGateway.getMassOutages(validAddress.state))
                .thenThrow(fallbackException)

        whenever(serviceOutageGateway
                .getFixedOutage(validAddress))
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.ADSL.name,
                        description = "ADSL is down",
                        impacted_services = "Data",
                        cause = Cause.POWER,
                        end = now, technology = "ADSL",
                        product_type = "Anything"
                )
                ),""))


        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("POWER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("ADSL is down"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("Anything"))
                .andExpect(jsonPath("massStateOutage.title").value(""))
                .andExpect(jsonPath("massStateOutage.description").value(""))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway).getMassOutages(validAddress.state)
        verify(serviceOutageGateway).getFixedOutage(validAddress)
    }

    @Test
    fun getServiceOutages_whenAddressHasNoProductType_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(validAddress)

        whenever(serviceOutageGateway
                .getFixedOutage(validAddress))
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.ADSL.name,
                        description = "ADSL is down",
                        impacted_services = "Data",
                        cause = Cause.POWER,
                        end = now, technology = "ADSL",
                        product_type = null
                )
                ),""))


        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("POWER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("ADSL is down"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("Telstra Business Broadband"))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
        verify(serviceOutageGateway).getMassOutages(validAddress.state)
        verify(serviceOutageGateway).getFixedOutage(validAddress)
    }

    @Test
    fun getServiceOutages_whenAddressVerifiedIsFalse_returnsInvalidAddress() {
        whenever(addressVerificationGateway.verifyAddress("345 Bad St")).thenReturn(InvalidAddress())

        mockMvc.perform(
                get("/outages/fixed?address=345 Bad St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("validAddress").value(false))

        verify(addressVerificationGateway).verifyAddress("345 Bad St")
    }

    @Test
    fun getServiceOutages_whenAddressHasNoOutage_returnsNoOutage() {
        whenever(addressVerificationGateway.verifyAddress("123 Good St")).thenReturn(validAddress)
        whenever(serviceOutageGateway.getFixedOutage(validAddress)).thenReturn(ServiceOutageData(false,listOf(),""))

        mockMvc.perform(
                get("/outages/fixed?address=123 Good St").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(0))

        verify(addressVerificationGateway).verifyAddress("123 Good St")
        verify(serviceOutageGateway).getFixedOutage(validAddress)
    }

    @Test
    fun getServiceOutages_whenNoAddressGiven_returns400() {
        given()
            .header("Recaptcha-Token", "token")
        `when`()
            .get("/outages/fixed")
        .then()
            .statusCode(400)
    }

    @Test
    fun getServiceOutages_whenBlankAddressGiven_returns400() {
        mockMvc.perform(
                get("/outages/fixed?address=").header("Recaptcha-Token", "token")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun getFixedOutages_whenFallbackFails_returns503() {
        whenever(addressVerificationGateway.verifyAddress("123 Good St")).thenReturn(validAddress)
        val fallbackException = HystrixRuntimeException(null, null, null, null, Exception(Exception(FallbackServiceOutageClientException())))
        whenever(serviceOutageGateway.getFixedOutage(validAddress)).thenThrow(fallbackException)

        mockMvc.perform(
                get("/outages/fixed?address=123 Good St").header("Recaptcha-Token", "token")
        ).andExpect(status().isServiceUnavailable)
    }

    @Test
    fun getMobileOutages_whenAddressHasOutage_returnsOutage() {
        whenever(serviceOutageGateway.getMobileOutage("BELCONNEN, 2617 ACT")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.MOBILE.name,
                                description = "MOBILE is down",
                                impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                product_type = "",
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))),
                                technology = "Mobile",
                                etr = DateConverter.toDateTime("2017-08-30T18:02:00 Australia/Sydney")
                        )
                )
        )

         `when`()
            .get("/outages/mobile?location=BELCONNEN, 2617 ACT")
        .then()
            .statusCode(200)
            .body("serviceStatus.size()", equalTo(1))
            .body("serviceStatus[0].name", equalTo("MOBILE"))
            .body("serviceStatus[0].cause", equalTo("MAINTENANCE"))
            .body("serviceStatus[0].description", equalTo("MOBILE is down"))
            .body("serviceStatus[0].latitude", notNullValue())
            .body("serviceStatus[0].impacted_services", equalTo("Data"))
            .body("serviceStatus[0].longitude", notNullValue())
            .body("serviceStatus[0].coverage.size()", equalTo(1))
            .body("serviceStatus[0].coverage[0].points.size()", equalTo(1))
            .body("serviceStatus[0].technology", equalTo("Mobile"))
            .body("serviceStatus[0].product_type", equalTo("Telstra Business Broadband"))
                 .body("serviceStatus[0].eta", notNullValue())


        verify(serviceOutageGateway).getMobileOutage("BELCONNEN, 2617 ACT")
    }

    @Test
    fun getMobileOutages_whenAddressHasNoStateOutage_returnsOutage() {

        whenever(serviceOutageGateway.getMassOutages("NSW"))
                .thenReturn(listOf())

        whenever(serviceOutageGateway.getMobileOutage("HACKETT, 2000 NSW")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.MOBILE.name,
                                description = "MOBILE is down",
                                impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))), technology = "Mobile",
                                product_type = "",
                                etr = DateConverter.toDateTime("2017-08-30T18:02:00 Australia/Sydney")
                        )
                )
        )

        `when`()
                .get("/outages/mobile?location=HACKETT, 2000 NSW")
                .then()
                .statusCode(200)
                .body("serviceStatus.size()", equalTo(1))
                .body("serviceStatus[0].name", equalTo("MOBILE"))
                .body("serviceStatus[0].cause", equalTo("MAINTENANCE"))
                .body("serviceStatus[0].description", equalTo("MOBILE is down"))
                .body("serviceStatus[0].latitude", notNullValue())
                .body("serviceStatus[0].technology",equalTo("Mobile"))
                .body("serviceStatus[0].impacted_services", equalTo("Data"))
                .body("serviceStatus[0].longitude", notNullValue())
                .body("serviceStatus[0].coverage.size()", equalTo(1))
                .body("serviceStatus[0].coverage[0].points.size()", equalTo(1))
                .body("massStateOutage.title", equalTo(""))
                .body("massStateOutage.description", equalTo(""))
                .body("serviceStatus[0].eta", notNullValue())

        verify(serviceOutageGateway).getMassOutages("NSW")

        verify(serviceOutageGateway).getMobileOutage("HACKETT, 2000 NSW")
    }

    @Test
    fun getMobileOutages_whenAddressHasStateOutage_returnsOutage() {

        whenever(serviceOutageGateway.getMassOutages("NSW"))
                .thenReturn(listOf(MassOutage("NSW Outage", description = "There is an outage in NSW")))

        whenever(serviceOutageGateway.getMobileOutage("HACKETT, 2000 NSW")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.MOBILE.name,
                                description = "MOBILE is down",
                                impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))), technology = "Mobile",
                                product_type = "",
                                etr = DateConverter.toDateTime("2017-08-30T18:02:00 Australia/Sydney")
                        )
                )
        )

        `when`()
                .get("/outages/mobile?location=HACKETT, 2000 NSW")
                .then()
                .statusCode(200)
                .body("serviceStatus.size()", equalTo(1))
                .body("massStateOutage.title", equalTo("NSW Outage"))
                .body("massStateOutage.description", equalTo("There is an outage in NSW"))
                .body("serviceStatus[0].eta", notNullValue())

        verify(serviceOutageGateway).getMassOutages("NSW")
    }

    @Test
    fun getMobileOutages_whenMassOutageReturnsException_returnsOutage() {

        val fallbackException = HystrixRuntimeException(null, null, null, null, Exception(Exception(FallbackServiceOutageClientException())))

        whenever(serviceOutageGateway.getMassOutages("NSW"))
                .thenThrow(fallbackException)

        whenever(serviceOutageGateway.getMobileOutage("HACKETT, 2000 NSW")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.MOBILE.name,
                                description = "MOBILE is down",
                                impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))), technology = "Mobile",
                                product_type = "",
                                etr = DateConverter.toDateTime("2017-08-30T18:02:00 Australia/Sydney")
                        )
                )
        )

        `when`()
                .get("/outages/mobile?location=HACKETT, 2000 NSW")
                .then()
                .statusCode(200)
                .body("serviceStatus.size()", equalTo(1))
                .body("serviceStatus[0].name", equalTo("MOBILE"))
                .body("serviceStatus[0].cause", equalTo("MAINTENANCE"))
                .body("serviceStatus[0].description", equalTo("MOBILE is down"))
                .body("serviceStatus[0].latitude", notNullValue())
                .body("serviceStatus[0].technology",equalTo("Mobile"))
                .body("serviceStatus[0].impacted_services", equalTo("Data"))
                .body("serviceStatus[0].longitude", notNullValue())
                .body("serviceStatus[0].coverage.size()", equalTo(1))
                .body("serviceStatus[0].coverage[0].points.size()", equalTo(1))
                .body("massStateOutage.title", equalTo(""))
                .body("massStateOutage.description", equalTo(""))
                .body("serviceStatus[0].eta", notNullValue())

        verify(serviceOutageGateway).getMassOutages("NSW")

        verify(serviceOutageGateway).getMobileOutage("HACKETT, 2000 NSW")
    }

    @Test
    fun getMobileOutages_whenAddressHasMassOutageIsCalledFalsy_returnsOutage() {

        val serviceOutagecontroller = ServiceOutageController(serviceOutageGateway, addressVerificationGateway, false)
        this.mockMvc = MockMvcBuilders.standaloneSetup(serviceOutagecontroller).setControllerAdvice(ResponseEntityExceptionHandler()).build()
        RestAssuredMockMvc.mockMvc(mockMvc)

        whenever(serviceOutageGateway.getMassOutages("NSW"))
                .thenReturn(listOf(MassOutage("NSW Outage", description = "There is an outage in NSW")))

        whenever(serviceOutageGateway.getMobileOutage("HACKETT, 2000 NSW")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.MOBILE.name,
                                description = "MOBILE is down",
                                impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))), technology = "Mobile",
                                product_type = "",
                                etr = DateConverter.toDateTime("2017-08-30T18:02:00 Australia/Sydney")
                        )
                )
        )

        `when`()
                .get("/outages/mobile?location=HACKETT, 2000 NSW")
                .then()
                .statusCode(200)
                .body("serviceStatus.size()", equalTo(1))
                .body("massStateOutage.title", equalTo(""))
                .body("massStateOutage.description", equalTo(""))
                .body("serviceStatus[0].eta", notNullValue())

        verify(serviceOutageGateway, never()).getMassOutages("NSW")
    }

    @Test
    fun getMobileOutages_whenAddressHasNoOutage_returnsNoOutage() {
        `when`()
            .get("/outages/mobile?location=HACKETT, 2000 NSW")
        .then()
            .statusCode(200)
            .body("serviceStatus.size()", equalTo(0))

        verify(serviceOutageGateway).getMobileOutage("HACKETT, 2000 NSW")
    }

    @Test
    fun getMobileOutages_whenNoLocationGiven_returns400() {
        `when`()
            .get("/outages/mobile")
        .then()
            .statusCode(400)
    }

    @Test
    fun getMobileOutages_whenBlankLocationGiven_returns400() {
        `when`()
            .get("/outages/mobile?location=")
        .then()
            .statusCode(400)
    }

    @Test
    fun getMobileOutages_whenLocationHasOutage_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(serviceOutageGateway
                .getMobileOutage("HACKETT, 2602 ACT"))
                .thenReturn(listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.MOBILE.name,
                        description = "Mobile is down",
                        impacted_services = "Data",
                        cause = Cause.OTHER,
                        end = now,
                        etr = now,
                        technology = "Mobile",
                        product_type = "")
                ))

        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/mobile?location=HACKETT, 2602 ACT")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("MOBILE"))
                .andExpect(jsonPath("serviceStatus[0].technology").value("Mobile"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("OTHER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("Mobile is down"))
                .andExpect(jsonPath("serviceStatus[0].impacted_services").value("Data"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("Telstra Business Broadband"))
                .andExpect(jsonPath("serviceStatus[0].eta").exists())

        verify(serviceOutageGateway).getMobileOutage("HACKETT, 2602 ACT")

    }

    @Test
    fun getMassOutages_returns200() {
        `when`()
                .get("/outages/mass-outages?state=NAT")
        .then()
                .statusCode(200)

        verify(serviceOutageGateway).getMassOutages("NAT")
    }

    @Test
    fun getMassOutages_whenStateIsEmpty_returns400() {
        `when`()
                .get("/outages/mass-outages?state=")
        .then()
                .statusCode(400)

    }

    @Test
    fun getMassOutages_returnsNationalOutages() {

        whenever(serviceOutageGateway
                .getMassOutages("NAT"))
                .thenReturn(listOf(MassOutage("NBN Issue Nationally", "<p><span style=\"color:black\"> NBN Issue Nationally </span></p>")
                ))

        mockMvc.perform(
                get("/outages/mass-outages?state=NAT")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("massOutage.size()").value(1))
                .andExpect(jsonPath("massOutage[0].title").value("NBN Issue Nationally"))
                .andExpect(jsonPath("massOutage[0].description").value("<p><span style=\"color:black\"> NBN Issue Nationally </span></p>"))

        verify(serviceOutageGateway).getMassOutages("NAT")
    }

    @Test
    fun getNationalMassOutages_whenMassOutageCalledIsFalsy_returnsEmptyMassOutage(){

        val serviceOutagecontroller = ServiceOutageController(serviceOutageGateway, addressVerificationGateway, false)
        this.mockMvc = MockMvcBuilders.standaloneSetup(serviceOutagecontroller).setControllerAdvice(ResponseEntityExceptionHandler()).build()
        RestAssuredMockMvc.mockMvc(mockMvc)

        whenever(serviceOutageGateway
                .getMassOutages("NAT"))
                .thenReturn(listOf(MassOutage("NBN Issue Nationally", "<p><span style=\"color:black\"> NBN Issue Nationally </span></p>")
                ))

        mockMvc.perform(
                get("/outages/mass-outages?state=NAT")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("massOutage.size()").value(0))

    }

    @Test
    fun getEmailOutage_whenHasOutage_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(serviceOutageGateway
                .getEmailOutage())
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        clock = clock,
                        name = TechnologyType.EMAIL.name,
                        description = "Customers may experience issues with their email service.",
                        impacted_services = "",
                        cause = Cause.POWER,
                        end = now, technology = "Email",
                        product_type = "")
                ),""))

        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/email").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("EMAIL"))
                .andExpect(jsonPath("serviceStatus[0].technology").value("Email"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("POWER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("Customers may experience issues with their email service."))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("Telstra Business Broadband"))

        verify(serviceOutageGateway).getEmailOutage()
    }


    @Test
    fun getEntertainmentOutages_whenSuburbHasOutage_returnsOutage() {
        whenever(serviceOutageGateway.getEntertainmentOutage("HACKETT, 2000 NSW", "ENTERTAINMENT")).thenReturn(
                listOf(
                        ServiceOutage(
                                clock = clock,
                                name = TechnologyType.FOXTEL.name,
                                description = "Foxtel is down",
                                //impacted_services = "Data",
                                cause = Cause.MAINTENANCE,
                                product_type = "",
                                latitude = 1.23,
                                longitude = 2.34,
                                coverage = listOf(Polygon(listOf(Point(1.21,3.25)))), technology = TechnologyType.FOXTEL.name
                        )
                )
        )

        `when`()
                .get("/outages/entertainment?location=HACKETT, 2000 NSW")
                .then()
                .statusCode(200)
                .body("serviceStatus.size()", equalTo(1))
                .body("serviceStatus[0].name", equalTo("FOXTEL"))
                .body("serviceStatus[0].cause", equalTo("MAINTENANCE"))
                .body("serviceStatus[0].description", equalTo("Foxtel is down"))
                .body("serviceStatus[0].technology", equalTo("FOXTEL"))
                .body("serviceStatus[0].product_type", equalTo("Telstra Business Broadband"))


        verify(serviceOutageGateway).getEntertainmentOutage("HACKETT, 2000 NSW", "ENTERTAINMENT")
    }

    @Test
    fun getAdslOutages_whenNoIdGiven_returns400() {
        `when`()
                .get("/outages/fixed/serviceid?id=")
                .then()
                .statusCode(400)
    }

    @Test
    fun getAdslOutages_whenNoParamGiven_returns400() {
        `when`()
                .get("/outages/fixed/serviceid?")
                .then()
                .statusCode(400)
    }

    @Test
    fun getAdslOutages_whenServiceIdHasNoOutage_returnsNoOutage() {
        whenever(serviceOutageGateway.getFixedOutageByServiceIdForAdsl("xyz@bigpond.com")).thenReturn(ServiceOutageData(false,listOf(),""))
        mockMvc.perform(
                get("/outages/fixed/serviceid?id=xyz@bigpond.com&idtype=ADSL&servicetype=ADSL").header("Recaptcha-Token", "token")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(0))

        verify(serviceOutageGateway).getFixedOutageByServiceIdForAdsl("xyz@bigpond.com")
    }

    @Test
    fun getAdslOutages_whenServiceIdHasOutage_returnsOutage() {
        val now = Instant.now().atZone(Clock.ZONE_ID)
        whenever(serviceOutageGateway
                .getFixedOutageByServiceIdForAdsl("xyz@bigpond.com"))
                .thenReturn(ServiceOutageData(true, listOf(ServiceOutage(
                        name = TechnologyType.ADSL.name,
                        description = "Adsl is down",
                        clock = clock,
                        cause = Cause.OTHER,
                        technology = "ADSL",
                        product_type = "",
                        impacted_services = "Data",
                        end = now
                )
                ),""))

        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)

        mockMvc.perform(
                get("/outages/fixed/serviceid?id=xyz@bigpond.com&idtype=ADSL&servicetype=ADSL")
        )
                .andExpect(status().isOk)
                .andExpect(jsonPath("serviceStatus.size()").value(1))
                .andExpect(jsonPath("serviceStatus[0].name").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].technology").value("ADSL"))
                .andExpect(jsonPath("serviceStatus[0].cause").value("OTHER"))
                .andExpect(jsonPath("serviceStatus[0].eta").value(timestamp))
                .andExpect(jsonPath("serviceStatus[0].description").value("Adsl is down"))
                .andExpect(jsonPath("serviceStatus[0].product_type").value("Telstra Business Broadband"))
                .andExpect(jsonPath("serviceStatus[0].eta").exists())

        verify(serviceOutageGateway).getFixedOutageByServiceIdForAdsl("xyz@bigpond.com")
    }
}

 */