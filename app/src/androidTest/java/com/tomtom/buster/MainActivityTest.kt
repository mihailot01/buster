package com.tomtom.buster

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.tomtom.buster.view.MAP_FRAGMENT_TAG
import com.tomtom.buster.view.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MainActivityTest(private val permissionRule: GrantPermissionRule) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<GrantPermissionRule> {
            return listOf(
                GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                GrantPermissionRule.grant(),
                GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testPermissions() {
        val scenario = scenarioRule.scenario

        scenario.onActivity { activity ->
            val context = activity.applicationContext
            val fragmentManager = activity.supportFragmentManager
            val mapFragment: Fragment? = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)

            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                assertNotNull(mapFragment)
            } else {
                assertNull(mapFragment)
            }
        }
    }
}
