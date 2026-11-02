package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityCreateGroupBinding

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private val groupManager = GroupManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.create_group)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty()
            val description = binding.etDescription.text?.toString().orEmpty()
            binding.progress.visibility = View.VISIBLE
            launchIo(onError = {
                binding.progress.visibility = View.GONE
                toast(it)
            }) {
                val group = groupManager.createGroup(name, description)
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    startActivity(
                        Intent(this@CreateGroupActivity, GroupDetailActivity::class.java)
                            .putExtra(HomeActivity.EXTRA_GROUP_ID, group.id)
                    )
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
