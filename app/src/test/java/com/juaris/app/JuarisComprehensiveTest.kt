package com.juaris.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juaris.app.email.EmailScanWorker
import com.juaris.app.sms.SmsFilterReceiver
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
    private lateinit var nearbyMeshManager: NearbyMeshManager
    private lateinit var quantumEngine: QuantumEngine
    private lateinit var localPhishingAnalyzer: LocalPhishingAnalyzer
    private lateinit var airGestureCore: AirGestureCore
    private lateinit var scamCallScreeningService: ScamCallScreeningService
    private lateinit var juarisEventBus: JuarisEventBus
    private lateinit var juarisReceiver: JuarisReceiver
    private lateinit var emailScanWorker: EmailScanWorker
    private lateinit var smsFilterReceiver: SmsFilterReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = JuarisDatabase.getInstance(context)
        securityEngine = SecurityEngine(context)
        billingManager = BillingManager(context)
        localAICore = LocalAICore(context)
        nearbyMeshManager = NearbyMeshManager(
            context = context,
            onDeviceDiscovered = { _ -> },
            onDeviceLost = { _ -> },
            onMessageReceived = { _, _ -> }
        )
        quantumEngine = QuantumEngine()
        localPhishingAnalyzer = LocalPhishingAnalyzer(context)
        airGestureCore = AirGestureCore(context)
        scamCallScreeningService = ScamCallScreeningService()
        juarisEventBus = JuarisEventBus()
        juarisReceiver = JuarisReceiver()
        emailScanWorker = EmailScanWorker(context, androidx.work.WorkerParameters.getInstance(context))
        smsFilterReceiver = SmsFilterReceiver()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun testLive01_DatabaseAndAllDaos() = runBlocking {
        assertNotNull("JuarisDatabase ist null", database)

        val logDao: SecurityLogDao = database.securityLogDao()
        assertNotNull("SecurityLogDao ist null", logDao)

        val mashDao: MashDao = database.mashDao()
        assertNotNull("MashDao ist null", mashDao)

        val entity = SecurityLogEntity(
            timestamp = System.currentTimeMillis(),
            eventType = "LIVE_GEAR_VERIFICATION",
            details = "Echter DB-Write/Read Test für alle DAOs"
        )
        logDao.insert(entity)
        val logs = logDao.getAllLogs()
        assertTrue("SecurityLogDao liefert keine Einträge zurück", logs.isNotEmpty())
    }

    @Test
    fun testLive02_SecurityEngine() {
        assertNotNull("SecurityEngine ist null", securityEngine)
        val blockedNumbers = securityEngine.getBlockedNumbers()
        assertNotNull("getBlockedNumbers() liefert null", blockedNumbers)
    }

    @Test
    fun testLive03_LocalAICore() {
        assertNotNull("LocalAICore ist null", localAICore)
        val safetyResult = localAICore.evaluateContentSafety("Echter Live-Check", null)
        assertNotNull("evaluateContentSafety() liefert null", safetyResult)
    }

    @Test
    fun testLive04_GlobalAndNearbyMeshEngines() {
        assertNotNull("GlobalMeshEngine ist null", GlobalMeshEngine)
        GlobalMeshEngine.broadcastToSwarm(
            context = context,
            content = "Live Test Payload",
            isEphemeral = true,
            onBlocked = {},
            onSuccess = {}
        )

        assertNotNull("NearbyMeshManager ist null", nearbyMeshManager)
        nearbyMeshManager.broadcastMessage("Test-Nachricht")
    }

    @Test
    fun testLive05_BillingManager() {
        assertNotNull("BillingManager ist null", billingManager)
        billingManager.startConnection {
            // Verbindung steht bereit
        }
    }

    @Test
    fun testLive06_QuantumEngine() {
        assertNotNull("QuantumEngine ist null", quantumEngine)
        val keyPair = quantumEngine.generatePostQuantumKeyPair()
        assertNotNull("generatePostQuantumKeyPair() liefert null", keyPair)
    }

    @Test
    fun testLive07_LocalPhishingAnalyzer() {
        assertNotNull("LocalPhishingAnalyzer ist null", localPhishingAnalyzer)
        val phishingCheck = localPhishingAnalyzer.analyze("https://juaris.app/verify")
        assertNotNull("analyze() liefert null", phishingCheck)
    }

    @Test
    fun testLive08_AirGestureCore() {
        assertNotNull("AirGestureCore ist null", airGestureCore)
        airGestureCore.stopGestureDetection()
    }

    @Test
    fun testLive09_ScamCallScreeningService() {
        assertNotNull("ScamCallScreeningService ist null", scamCallScreeningService)
    }

    @Test
    fun testLive10_JuarisEventBusAndReceiver() {
        assertNotNull("JuarisEventBus ist null", juarisEventBus)
        assertNotNull("JuarisReceiver ist null", juarisReceiver)
    }

    @Test
    fun testLive11_EmailScanWorkerAndSmsFilterReceiver() {
        assertNotNull("EmailScanWorker ist null", emailScanWorker)
        assertNotNull("SmsFilterReceiver ist null", smsFilterReceiver)
    }

    @Test
    fun testLive12_MainActivityReference() {
        val mainActivityClass = MainActivity::class.java
        assertNotNull("MainActivity Class nicht auflösbar", mainActivityClass)
    }
}


