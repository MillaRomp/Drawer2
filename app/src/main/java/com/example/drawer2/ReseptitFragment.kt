package com.example.drawer2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import kotlin.random.Random

class ReseptitFragment : Fragment(R.layout.fragment_reseptit) {

    private lateinit var etSearch: EditText
    private lateinit var containerRecipesList: LinearLayout
    private lateinit var scrollResultsList: ScrollView
    private lateinit var cardRecipeDetails: View
    private lateinit var ivFullImage: ImageView
    private lateinit var tvFullTitle: TextView
    private lateinit var tvFullIngredients: TextView
    private lateinit var tvFullInstructions: TextView
    
    private val apiKey = "1"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etRecipeSearch)
        containerRecipesList = view.findViewById(R.id.containerRecipesList)
        scrollResultsList = view.findViewById(R.id.scrollResultsList)
        cardRecipeDetails = view.findViewById(R.id.cardRecipeDetails)
        ivFullImage = view.findViewById(R.id.ivRecipeFullImage)
        tvFullTitle = view.findViewById(R.id.tvRecipeTitle)
        tvFullIngredients = view.findViewById(R.id.tvIngredientsList)
        tvFullInstructions = view.findViewById(R.id.tvRecipeInstructions)

        view.findViewById<Button>(R.id.btnSearchRecipe).setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchRecipes("https://www.themealdb.com/api/json/v1/$apiKey/search.php?s=${URLEncoder.encode(query, "UTF-8")}")
            } else {
                context?.let { Toast.makeText(it, "Kirjoita hakusana!", Toast.LENGTH_SHORT).show() }
            }
        }

        view.findViewById<Button>(R.id.btnRandomRecipe).setOnClickListener {
            searchRecipes("https://www.themealdb.com/api/json/v1/$apiKey/random.php")
        }

        view.findViewById<Button>(R.id.btnBackToResults).setOnClickListener {
            showListView()
        }
    }

    private fun searchRecipes(urlString: String) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        showListView()
        containerRecipesList.removeAllViews()
        containerRecipesList.addView(TextView(context).apply { text = "Haetaan..." })

        executor.execute {
            try {
                val response = URL(urlString).readText()
                val jsonObject = JSONObject(response)
                val meals = jsonObject.optJSONArray("meals")

                handler.post {
                    containerRecipesList.removeAllViews()
                    if (meals != null && meals.length() > 0) {
                        for (i in 0 until meals.length()) {
                            val meal = meals.getJSONObject(i)
                            addRecipeToList(meal)
                        }
                    } else {
                        containerRecipesList.addView(TextView(context).apply { text = "Ei tuloksia." })
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    containerRecipesList.removeAllViews()
                    containerRecipesList.addView(TextView(context).apply { text = "Virhe yhteydessä." })
                }
            }
        }
    }

    private fun addRecipeToList(meal: JSONObject) {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_recipe, containerRecipesList, false)
        
        val tvName = itemView.findViewById<TextView>(R.id.tvRecipeName)
        val ivThumb = itemView.findViewById<ImageView>(R.id.ivRecipeThumbnail)

        tvName.text = meal.getString("strMeal")
        val thumbUrl = meal.getString("strMealThumb")

        // Lisätty Crossfade-siirtymä pehmeää latausta varten
        context?.let {
            Glide.with(it)
                .load(thumbUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.nav3_reseptit)
                .into(ivThumb)
        }

        itemView.setOnClickListener {
            showDetails(meal)
        }

        containerRecipesList.addView(itemView)
    }

    private fun showDetails(meal: JSONObject) {
        scrollResultsList.visibility = View.GONE
        cardRecipeDetails.visibility = View.VISIBLE

        tvFullTitle.text = meal.getString("strMeal")
        tvFullInstructions.text = meal.getString("strInstructions")
        
        val thumbUrl = meal.getString("strMealThumb")
        // Lisätty Crossfade-siirtymä myös suurelle kuvalle
        context?.let { 
            Glide.with(it)
                .load(thumbUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivFullImage) 
        }

        val ingredients = StringBuilder()
        for (i in 1..20) {
            val ing = meal.optString("strIngredient$i")
            val meas = meal.optString("strMeasure$i")
            if (!ing.isNullOrBlank() && ing != "null") {
                ingredients.append("• $meas $ing\n")
            }
        }
        tvFullIngredients.text = ingredients.toString().trim()
    }

    private fun showListView() {
        scrollResultsList.visibility = View.VISIBLE
        cardRecipeDetails.visibility = View.GONE
    }
}