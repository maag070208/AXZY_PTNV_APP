package com.axzydev.puertonuevoapp.feature.access

import com.axzydev.puertonuevoapp.core.location.DeviceLocation
import com.axzydev.puertonuevoapp.core.location.LocationProvider
import com.axzydev.puertonuevoapp.core.network.access.AccessApi
import com.axzydev.puertonuevoapp.core.network.access.AccessEventDto
import com.axzydev.puertonuevoapp.core.network.access.AccessEventInput
import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.network.access.AccessLastEventDto
import com.axzydev.puertonuevoapp.core.network.access.AccessLookupResultDto
import com.axzydev.puertonuevoapp.core.network.access.AccessStatusDto
import com.axzydev.puertonuevoapp.core.network.access.AccessTodayResponseDto
import com.axzydev.puertonuevoapp.core.network.access.SiteDto
import com.axzydev.puertonuevoapp.core.network.http.ApiException
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccessScanViewModelTest {

    private val validQr = """{"v":2,"id":"emp-1","name":"Ana Palma"}"""

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        api: FakeAccessApi = FakeAccessApi(),
        location: DeviceLocation? = DeviceLocation(20.6, -103.3, 12.0),
    ): Pair<AccessScanViewModel, FakeAccessApi> {
        val viewModel = AccessScanViewModel(api, FakeLocationProvider(location))
        return viewModel to api
    }

    @Test
    fun loadsSitesWithoutPreselecting() = runTest {
        val (viewModel, _) = newViewModel()
        val state = viewModel.uiState.value
        assertEquals(false, state.loadingSites)
        assertEquals(1, state.sites.size)
        assertNull(state.selectedSiteId)
    }

    @Test
    fun scanningWithoutSiteIsBlocked() = runTest {
        val (viewModel, api) = newViewModel()
        viewModel.onQrScanned(validQr)

        assertNotNull(viewModel.uiState.value.scanError)
        assertNull(viewModel.uiState.value.employee)
        assertEquals(ScanPhase.SCANNING, viewModel.uiState.value.phase)
        assertEquals(0, api.createInputs.size)
    }

    @Test
    fun invalidQrStaysScanningWithError() = runTest {
        val (viewModel, _) = newViewModel()
        viewModel.selectSite("site-1")
        viewModel.onQrScanned("no es un qr")

        assertEquals(ScanPhase.SCANNING, viewModel.uiState.value.phase)
        assertNotNull(viewModel.uiState.value.scanError)
        assertNull(viewModel.uiState.value.employee)
    }

    @Test
    fun validQrReachesReviewWithServerSuggestion() = runTest {
        val (viewModel, api) = newViewModel()
        api.lookupResult = Result.success(
            sampleLookup(
                suggestedType = AccessEventType.EXIT,
                lastEvent = AccessLastEventDto("evt-0", AccessEventType.ENTRY, "2026-09-22T09:00:00.000Z"),
            )
        )

        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)

        val state = viewModel.uiState.value
        assertEquals(ScanPhase.REVIEW, state.phase)
        assertEquals("emp-1", state.employee?.id)
        assertEquals(AccessEventType.EXIT, state.selectedType)
        assertNotNull(state.pendingClientEventId)
    }

    @Test
    fun lookupFailureReturnsToScanning() = runTest {
        val (viewModel, api) = newViewModel()
        api.lookupResult = Result.failure(ApiException(404, "No existe un empleado con la credencial escaneada"))

        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)

        val state = viewModel.uiState.value
        assertEquals(ScanPhase.SCANNING, state.phase)
        assertTrue(state.scanError!!.contains("No existe"))
        assertNull(state.pendingClientEventId)
    }

    @Test
    fun confirmRegistersWithGpsAndSucceeds() = runTest {
        val (viewModel, api) = newViewModel()
        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)
        viewModel.confirm()

        val state = viewModel.uiState.value
        assertEquals(ScanPhase.SUCCESS, state.phase)
        assertEquals("GPS", state.locationSource)
        assertNotNull(state.result)
        assertEquals(1, api.createInputs.size)
        assertEquals("emp-1", api.createInputs.first().employeeId)
        assertEquals("site-1", api.createInputs.first().siteId)
        assertEquals(20.6, api.createInputs.first().latitude)
    }

    @Test
    fun confirmWithoutGpsFallsBackToSiteOnly() = runTest {
        val (viewModel, _) = newViewModel(location = null)
        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)
        viewModel.confirm()

        assertEquals(ScanPhase.SUCCESS, viewModel.uiState.value.phase)
        assertEquals("SITE_ONLY", viewModel.uiState.value.locationSource)
    }

    @Test
    fun retryReusesSameClientEventId() = runTest {
        val (viewModel, api) = newViewModel()
        api.createResult = Result.failure(ApiException(500, "boom"))
        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)

        val generatedId = viewModel.uiState.value.pendingClientEventId
        viewModel.confirm()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(ScanPhase.REVIEW, viewModel.uiState.value.phase)

        api.createResult = Result.success(sampleEvent())
        viewModel.retryRegistration()

        assertEquals(ScanPhase.SUCCESS, viewModel.uiState.value.phase)
        assertEquals(2, api.createInputs.size)
        assertEquals(api.createInputs[0].clientEventId, api.createInputs[1].clientEventId)
        assertEquals(generatedId, api.createInputs[1].clientEventId)
    }

    @Test
    fun businessConflictMessageIsSurfaced() = runTest {
        val (viewModel, api) = newViewModel()
        api.createResult = Result.failure(ApiException(409, "El empleado ya tiene una entrada abierta"))
        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)
        viewModel.confirm()

        assertEquals(ScanPhase.REVIEW, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("entrada abierta"))
    }

    @Test
    fun scanNextResetsToScanning() = runTest {
        val (viewModel, _) = newViewModel()
        viewModel.selectSite("site-1")
        viewModel.onQrScanned(validQr)
        viewModel.confirm()

        viewModel.scanNext()

        val state = viewModel.uiState.value
        assertEquals(ScanPhase.SCANNING, state.phase)
        assertNull(state.employee)
        assertNull(state.pendingClientEventId)
        assertNull(state.result)
    }
}

private class FakeLocationProvider(private val location: DeviceLocation?) : LocationProvider {
    override suspend fun currentLocation(): DeviceLocation? = location
}

private class FakeAccessApi : AccessApi {
    var sitesResult: List<SiteDto> = listOf(SiteDto(id = "site-1", name = "Portería principal"))
    var lookupResult: Result<AccessLookupResultDto> = Result.success(sampleLookup())
    var createResult: Result<AccessEventDto> = Result.success(sampleEvent())
    val createInputs = mutableListOf<AccessEventInput>()

    override suspend fun lookup(qr: String): AccessLookupResultDto = lookupResult.getOrThrow()

    override suspend fun createEvent(input: AccessEventInput): AccessEventDto {
        createInputs += input
        return createResult.getOrThrow()
    }

    override suspend fun status(employeeId: String): AccessStatusDto = error("status no usado")

    override suspend fun query(request: TableRequest): TableResponse<AccessEventDto> =
        TableResponse(emptyList(), 0)

    override suspend fun sites(): List<SiteDto> = sitesResult

    override suspend fun meToday(): AccessTodayResponseDto = AccessTodayResponseDto(emptyList(), 0)

    override suspend fun photoBytes(path: String): ByteArray = ByteArray(0)
}

private fun sampleLookup(
    suggestedType: String = AccessEventType.ENTRY,
    lastEvent: AccessLastEventDto? = null,
): AccessLookupResultDto = AccessLookupResultDto(
    id = "emp-1",
    name = "Ana Palma",
    employeeNumber = "E-001",
    jobTitle = "Analista",
    department = "Sistemas",
    active = true,
    photoUrl = null,
    credentialVersion = 2,
    lastEvent = lastEvent,
    suggestedType = suggestedType,
)

private fun sampleEvent(): AccessEventDto = AccessEventDto(
    id = "evt-1",
    type = AccessEventType.ENTRY,
    occurredAt = "2026-09-22T10:00:00.000Z",
    employeeId = "emp-1",
    employeeNameSnapshot = "Ana Palma",
)
