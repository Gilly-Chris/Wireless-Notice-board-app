package com.example.wirelessnoticeboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.wirelessnoticeboard.Fragments.DocumentFragment
import com.example.wirelessnoticeboard.Fragments.ImageFragment
import com.example.wirelessnoticeboard.Fragments.TextFragment
import com.example.wirelessnoticeboard.Fragments.VideoFragment
import com.example.wirelessnoticeboard.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var drawerLayout : DrawerLayout
    private lateinit var actionbarToggle : ActionBarDrawerToggle
    private lateinit var navView : NavigationView
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth

        setSupportActionBar(binding.toolbar);
        drawerLayout = binding.drawerLayout
        actionbarToggle = ActionBarDrawerToggle(this, binding.drawerLayout, 0, 0)
        drawerLayout.addDrawerListener(actionbarToggle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        actionbarToggle.syncState()

        navView = binding.navView
        navView.menu.getItem(0).isChecked = true
        replaceFragment(TextFragment(), R.id.frame_layout)

        navView.setNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId){
               R.id.send_text_fragment -> {
                   replaceFragment(TextFragment(), R.id.frame_layout)
                   toggleDrawer()
                   true
               }
                R.id.send_image_fragment -> {
                    replaceFragment(ImageFragment(), R.id.frame_layout)
                    toggleDrawer()
                    true
                }
                R.id.send_video_fragment -> {
                    replaceFragment(VideoFragment(), R.id.frame_layout)
                    toggleDrawer()
                    true
                }
                R.id.send_document_fragment -> {
                    replaceFragment(DocumentFragment(), R.id.frame_layout)
                    toggleDrawer()
                    true
                }
                R.id.logout -> {
                    if(Util.isNetworkAvailable(this)){
                        Firebase.auth.signOut()
                        var intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                        startActivity(intent)
                        finish()
                    }else {
                        Toast.makeText(this, "No Internet Connection!!", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        if(!Util.isNetworkAvailable(this)){
            Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_LONG).show()
            return
        }
    }

    private fun toggleDrawer() {
        if(this.drawerLayout.isDrawerOpen(GravityCompat.START)){
            this.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        drawerLayout.openDrawer(navView)
        return true
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if(currentUser == null){
            var intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        if(this.drawerLayout.isDrawerOpen(GravityCompat.START)){
            this.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            return super.onBackPressed()
        }
    }

    fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int){
        supportFragmentManager.inTransaction { add(frameId, fragment) }
    }

    fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.inTransaction{replace(frameId, fragment)}
    }

    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
    }
}
