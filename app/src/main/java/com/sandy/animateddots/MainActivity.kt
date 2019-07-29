package com.sandy.animateddots

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    var count = 0;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        textView.text = dots.currentSelectedDots.toString()
        textView2.text = dots2.currentSelectedDots.toString()

        dots.visibleDots = 6
        minus.setOnClickListener {
            dots.removeCounter()
            textView.text = dots.currentSelectedDots.toString()
        }

        plus.setOnClickListener {
            dots.addCounter()
            textView.text = dots.currentSelectedDots.toString()
        }

        dots2.visibleDots = 6
        minus2.setOnClickListener {
            dots2.removeCounter()
            textView2.text = dots2.currentSelectedDots.toString()
        }

        plus2.setOnClickListener {
            dots2.addCounter()
            textView2.text = dots2.currentSelectedDots.toString()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
