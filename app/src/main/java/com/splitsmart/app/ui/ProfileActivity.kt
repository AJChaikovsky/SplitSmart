package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val accountManager = AccountManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.profile)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty()
            binding.progress.visibility = View.VISIBLE
            launchIo(onError = {
                binding.progress.visibility = View.GONE
                toast(it)
            }) {
                accountManager.updateName(name)
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    toast("Name updated")
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            accountManager.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        load()
    }

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val user = accountManager.getCurrentUser()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                if (user == null) {
                    toast("Not logged in")
                    finish()
                    return@runOnUiThread
                }
                binding.tvEmail.text = user.email
                binding.tvUscId.text = "USC ID: ${user.uscId}"
                binding.etName.setText(user.name)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
