package com.example.drawer2

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var isNavigating = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Poistetaan ikonien väripakotus
        bottomNav.itemIconTintList = null
        navView.itemIconTintList = null

        // Custom hamburger-painikkeen toiminnallisuus
        val btnHamburger = findViewById<ImageButton>(R.id.btn_hamburger_custom)
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (savedInstanceState == null) {
            updateSelection(R.id.nav_home, "Ostokset")
        }

        navView.setNavigationItemSelectedListener { item ->
            if (!isNavigating) {
                val title = when (item.itemId) {
                    R.id.nav_home -> "Ostokset"
                    R.id.nav_notes -> "Päiväkirja"
                    R.id.nav_settings -> "Reseptit"
                    else -> ""
                }
                updateSelection(item.itemId, title)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (!isNavigating && bottomNav.selectedItemId != item.itemId) {
                val title = when (item.itemId) {
                    R.id.nav_home -> "Ostokset"
                    R.id.nav_notes -> "Päiväkirja"
                    R.id.nav_settings -> "Reseptit"
                    else -> ""
                }
                updateSelection(item.itemId, title)
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    fun updateSelection(itemId: Int, title: String) {
        if (isNavigating) return
        isNavigating = true
        
        val fragment = when (itemId) {
            R.id.nav_home -> KauppaFragment()
            R.id.nav_notes -> PaivakirjaFragment()
            R.id.nav_settings -> ReseptitFragment()
            else -> KauppaFragment()
        }

        val toolbarImage = findViewById<ImageView>(R.id.toolbar_title_image)
        when (itemId) {
            R.id.nav_home -> toolbarImage.setImageResource(R.drawable.ostoks)
            R.id.nav_notes -> toolbarImage.setImageResource(R.drawable.paivaki_toolbar)
            R.id.nav_settings -> toolbarImage.setImageResource(R.drawable.resept_toolbar)
        }
        
        supportActionBar?.title = ""
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitNowAllowingStateLoss()

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        if (navView.checkedItem?.itemId != itemId) {
            navView.setCheckedItem(itemId)
        }
        if (bottomNav.selectedItemId != itemId) {
            bottomNav.selectedItemId = itemId
        }

        // Suurempi pompahdus-animaatio (1.5x koko)
        animateBottomIcon(bottomNav, itemId)
        
        isNavigating = false
    }

    private fun animateBottomIcon(bottomNav: BottomNavigationView, selectedId: Int) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            // Etsitään yksittäisen valintaelementin näkymä
            val view = bottomNav.findViewById<View>(item.itemId)
            
            if (item.itemId == selectedId) {
                // Voimakkaampi pompahdus (1.5x suuruus)
                view.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(250)
                    .start()
            } else {
                // Palautus normaalikokoon
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(250)
                    .start()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}