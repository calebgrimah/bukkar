package com.neoncoreng.bukkar.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoncoreng.bukkar.BukkarApiService
import com.neoncoreng.bukkar.BukkarApplication
import com.neoncoreng.bukkar.db.PartialRestaurant
import com.neoncoreng.bukkar.db.RestaurantsDb
import com.neoncoreng.bukkar.model.Restaurant
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.UnknownHostException

class RestaurantsViewmodel(private val stateHandle: SavedStateHandle) : ViewModel() {

    private var restInterface: BukkarApiService
    private var restaurantsDao = RestaurantsDb
        .getDaoInstance(
            BukkarApplication.getAppContext()
        )

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    init {
        val retrofit: Retrofit =
            Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BaseUrl)
                .build()
        restInterface = retrofit.create(
            BukkarApiService::class.java
        )
        getRestaurants()
    }

    val state = mutableStateOf(emptyList<Restaurant>())

    fun toggleFavorite(id: Int, oldValue: Boolean) {
        val restaurants = state.value.toMutableList()
        val itemIndex = restaurants.indexOfFirst { it.id == id }
        val item = restaurants[itemIndex]
        restaurants[itemIndex] = item.copy(isFavorite = !item.isFavorite)
        storeSelection(restaurants[itemIndex])
        state.value = restaurants
        viewModelScope.launch {
            val updatedRestaurants =
                toggleFavoriteRestaurant(id, oldValue)
            state.value = updatedRestaurants
        }
    }

    private fun storeSelection(restaurant: Restaurant) {
        val savedToggled = stateHandle.get<List<Int>?>(FAVORITES)
            .orEmpty().toMutableList()
        if (restaurant.isFavorite) savedToggled.add(restaurant.id!!) else savedToggled.remove(
            restaurant.id
        )
        stateHandle[FAVORITES] = savedToggled
    }

    companion object {
        const val FAVORITES = "favorites"
    }

    private suspend fun toggleFavoriteRestaurant(
        id: Int,
        oldValue: Boolean
    ) =
        withContext(Dispatchers.IO) {
            restaurantsDao.update(
                PartialRestaurant(
                    id = id,
                    isFavorite = !oldValue
                )
            )
            restaurantsDao.getAll()
        }

    private fun List<Restaurant>.restoreSelections(): List<Restaurant> {
        stateHandle.get<List<Int>?>(FAVORITES)?.let { selectedIds ->
            val restaurantsMap = this.associateBy { it.id }.toMutableMap()
            selectedIds.forEach { id ->
                val restaurant = restaurantsMap[id] ?: return@forEach
                restaurantsMap[id] = restaurant.copy(isFavorite = true)
            }
            return restaurantsMap.values.toList()
        }
        return this
    }

    private fun getRestaurants() {
        viewModelScope.launch(errorHandler) {
            state.value = getAllRestaurants()

        }

    }

    private suspend fun refreshCache() {
        val remoteRestaurants = restInterface
            .getRestaurants()
        val favoriteRestaurants = restaurantsDao
            .getAllFavorited()
        restaurantsDao.addAll(remoteRestaurants)
        restaurantsDao.updateAll(
            favoriteRestaurants.map {
                PartialRestaurant(it.id, true)
            })
    }

    private suspend fun getAllRestaurants(): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            try {
                refreshCache()
            } catch (e: Exception) {
                when (e) {
                    is UnknownHostException,
                    is ConnectException,
                    is HttpException -> {
                        if (restaurantsDao.getAll().isEmpty())
                            throw Exception(
                                "Something went wrong. " +
                                        "We have no data."
                            )
                    }
                    else -> throw e
                }
            }
            return@withContext restaurantsDao.getAll()
        }
    }

}

const val BaseUrl = "https://bukkar-9afac-default-rtdb.firebaseio.com/"