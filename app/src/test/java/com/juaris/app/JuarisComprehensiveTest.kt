package com.juaris.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JuarisComprehensiveTest {

    private lateinit var context: Context
    private lateinit var database: JuarisDatabase
    private lateinit var securityEngine: SecurityEngine
    private lateinit var billingManager: BillingManager
    private lateinit var localAICore: LocalAICore
    private lateinit var globalMeshEngine: GlobalMeshEngine
    private lateinit var nearbyMeshManager: NearbyMeshManager
    private lateinit var quantumEngine: QuantumEngine
    private lateinit var localPhishingAnalyzer: LocalPhishingAnalyzer
    private lateinit var airGestureCore: AirGestureCore
    private lateinit var scamCallScreeningService: ScamCallScreeningService
    private lateinit var juarisEventBus: JuarisEventBus
    private lateinit var juarisReceiver: JuarisReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = JuarisDatabase.getInstance(context)
        securityEngine = SecurityEngine(context)
        billingManager = BillingManager(context)
        localAICore = LocalAICore(context)
        globalMeshEngine = GlobalMeshEngine(context)
        nearbyMeshManager = NearbyMeshManager(context)
        quantumEngine = QuantumEngine()
        localPhishingAnalyzer = LocalPhishingAnalyzer(context)
        airGestureCore = AirGestureCore()
        scamCallScreeningService = ScamCallScreeningService()
        juarisEventBus = JuarisEventBus()
        juarisReceiver = JuarisReceiver()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun test01_DatabaseAndAllDaosLive() = runBlocking {
        assertNotNull("JuarisDatabase ist null", database)
        
        val logDao = database.securityLogDao()
        assertNotNull("SecurityLogDao ist null", logDao)

        val mashDao = database.mashDao()
        assertNotNull("MashDao ist null", mashDao)

        val entity = SecurityLogEntity(
            timestamp = System.currentTimeMillis(),
            eventType = "TOTAL_SYSTEM_CHECK",
            details = "Prüfung aller Tabellen und DAOs"
        )
        logDao.insert(entity)
        val logs = logDao.getAllLogs()
        assertTrue("Log-Tabelle konnte nicht beschrieben/gelesen werden", logs.isNotEmpty())
    }

    @Test
    fun test02_SecurityEngineLive() {
        assertNotNull("SecurityEngine ist null", securityEngine)
        val result = securityEngine.evaluateDeviceSecurity()
        assertNotNull("SecurityEngine.evaluateDeviceSecurity() lieferte null", result)
    }

    @Test
    fun test03_LocalAICoreLive() {
        assertNotNull("LocalAICore ist null", localAICore)
        assertTrue("LocalAICore nicht bereit", localAICore.isCoreReady())
        val analysis = localAICore.analyzeLocalThreat("Full system scan payload")
        assertNotNull("LocalAICore Analyse fehlgeschlagen", analysis)
    }

    @Test
    fun test04_GlobalMeshAndNearbyMeshLive() {
        assertNotNull("GlobalMeshEngine ist null", globalMeshEngine)
        assertNotNull("GlobalMeshNodeId ist null", globalMeshEngine.getMeshNodeId())

        assertNotNull("NearbyMeshManager ist null", nearbyMeshManager)
        val nearbyStatus = nearbyMeshManager.isMeshActive()
        assertNotNull("NearbyMeshManager Status ist null", nearbyStatus)
    }

    @Test
    fun test05_BillingManagerLive() {
        assertNotNull("BillingManager ist null", billingManager)
        val supported = billingManager.isBillingSupported()
        // Ruft die echte Billing-Schnittstelle ab
        assertNotNull("Billing Support Abfrage fehlgeschlagen", supported)
    }

    @Test
    fun test06_QuantumEngineLive() {
        assertNotNull("QuantumEngine ist null", quantumEngine)
        val hash = quantumEngine.generateSecureHash("Juaris-Total-Control-Test")
        assertEquals(64, hash.length)
    }

    @Test
    fun test07_LocalPhishingAnalyzerLive() {
        assertNotNull("LocalPhishingAnalyzer ist null", localPhishingAnalyzer)
        val isPhishing = localPhishingAnalyzer.isPhishingUrl("http://local-test-safe-url.juaris")
        assertFalse("Lokale sichere URL fälschlicherweise als Phishing markiert", isPhishing)
    }

    @Test
    fun test08_AirGestureCoreLive() {
        assertNotNull("AirGestureCore ist null", airGestureCore)
        airGestureCore.processSensorData(floatArrayOf(1.0f, -0.5f, 0.2f))
        val gestureState = airGestureCore.getCurrentGestureState()
        assertNotNull("AirGestureCore liefert keinen Zustand", gestureState)
    }

    @Test
    fun test09_ScamCallScreeningServiceLive() {
        assertNotNull("ScamCallScreeningService ist null", scamCallScreeningService)
    }

    @Test
    fun test10_EventBusAndReceiverLive() {
        assertNotNull("JuarisEventBus ist null", juarisEventBus)
        assertNotNull("JuarisReceiver ist null", juarisReceiver)
    }
}

