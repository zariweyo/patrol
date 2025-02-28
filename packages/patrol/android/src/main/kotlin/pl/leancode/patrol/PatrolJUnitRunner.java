// Terminology note:
// "Run a test" is used interchangeably with "execute a test".
// "Run a Dart test" is used interchangeably with "request execution of a Dart test" and "execute a Dart test".
// "ATO" is short for "Android Test Orchestrator".

package pl.leancode.patrol;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnitRunner;
import io.grpc.StatusRuntimeException;
import pl.leancode.patrol.contracts.Contracts.DartTestGroup;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static pl.leancode.patrol.contracts.Contracts.RunDartTestResponse;

/**
 * <p>
 * A customized AndroidJUnitRunner that enables Patrol on Android.
 * </p>
 */
public class PatrolJUnitRunner extends AndroidJUnitRunner {
    public PatrolAppServiceClient patrolAppServiceClient;

    @Override
    protected boolean shouldWaitForActivitiesToComplete() {
        return false;
    }

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);

        // This is only true when the ATO requests a list of tests from the app during the initial run.
        boolean isInitialRun = Boolean.parseBoolean(arguments.getString("listTestsForOrchestrator"));

        Logger.INSTANCE.i("--------------------------------");
        Logger.INSTANCE.i("PatrolJUnitRunner.onCreate() " + (isInitialRun ? "(initial run)" : ""));
    }

    /**
     * <p>
     * The native test runner needs to know what tests exist before it can execute them.
     * To gather the tests, the native test runner (by default: AndroidJUnitRunner) runs
     * the instrumentation during the ATO's initial run and collects the tests.
     * </p>
     *
     * <p>
     * This default behavior doesn't work with Flutter apps. That's because in Flutter
     * apps, the tests are in the app itself, so running only the instrumentation
     * during the initial run is not enough.
     * The app must also be run, and queried for Dart tests That's what this method does.
     * </p>
     */
    public void setUp(Class<?> activityClass) {
        Logger.INSTANCE.i("PatrolJUnitRunner.setUp(): activityClass = " + activityClass.getCanonicalName());

        // This code launches the app under test. It's based on ActivityTestRule#launchActivity.
        // It's simpler because we don't have the need for that much synchronization.
        // Currently, the only synchronization point we're interested in is when the app under test returns the list of tests.
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(instrumentation.getTargetContext(), activityClass.getCanonicalName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.getTargetContext().startActivity(intent);

        PatrolServer patrolServer = new PatrolServer();
        patrolServer.start(); // Gets killed when the instrumentation process dies. We're okay with this.

        patrolAppServiceClient = createAppServiceClient();
    }

    public PatrolAppServiceClient createAppServiceClient() {
        return new PatrolAppServiceClient();
    }

    /**
     * <p>
     * Waits until PatrolAppService, running in the Dart side of the app, reports
     * that it's ready to be asked about the list of Dart tests.
     * </p>
     *
     * <p>
     * PatrolAppService becomes ready once the special Dart test named "patrol_test_explorer" finishes running.
     * </p>
     */
    public void waitForPatrolAppService() {
        final String TAG = "PatrolJUnitRunner.setUp(): ";

        try {
            Logger.INSTANCE.i(TAG + "Waiting for PatrolAppService to report its readiness...");
            PatrolServer.Companion.getAppReadyFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.INSTANCE.e(TAG + "Exception was thrown when waiting for appReady: ", e);
            throw new RuntimeException(e);
        }

        Logger.INSTANCE.i(TAG + "PatrolAppService is ready to report Dart tests");
    }

    public Object[] listDartTests() {
        final String TAG = "PatrolJUnitRunner.listDartTests(): ";

        try {
            final DartTestGroup dartTestGroup = patrolAppServiceClient.listDartTests();
            Object[] dartTestFiles = ContractsExtensionsKt.listFlatDartFiles(dartTestGroup).toArray();
            Logger.INSTANCE.i(TAG + "Got Dart tests: " + Arrays.toString(dartTestFiles));
            return dartTestFiles;
        } catch (StatusRuntimeException e) {
            Logger.INSTANCE.e(TAG + "Failed to list Dart tests: ", e);
            throw e;
        }
    }

    /**
     * Requests execution of a Dart test and waits for it to finish.
     * Throws AssertionError if the test fails.
     */
    public RunDartTestResponse runDartTest(String name) {
        final String TAG = "PatrolJUnitRunner.runDartTest(" + name + "): ";

        try {
            Logger.INSTANCE.i(TAG + "Requested execution");
            RunDartTestResponse response = patrolAppServiceClient.runDartTest(name);
            if (response.getResult() == RunDartTestResponse.Result.FAILURE) {
                throw new AssertionError("Dart test failed: " + name + "\n" + response.getDetails());
            }
            return response;
        } catch (StatusRuntimeException e) {
            Logger.INSTANCE.e(TAG + e.getMessage(), e.getCause());
            throw e;
        }
    }
}
