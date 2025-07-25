/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.ui

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieDrawable
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.data.model.AppSettings
import org.calypsonet.keyple.demo.validation.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.validation.data.model.ReaderType
import org.calypsonet.keyple.demo.validation.data.model.Status
import org.calypsonet.keyple.demo.validation.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.validation.di.scope.ActivityScoped
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@ActivityScoped
class ReaderActivity : BaseActivity() {

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  @Suppress("DEPRECATION") private lateinit var progress: ProgressDialog
  private var cardReaderObserver: CardReaderObserver? = null
  var currentAppState = AppState.WAIT_SYSTEM_READY
  private var timer = Timer()

  /* application states */
  enum class AppState {
    WAIT_SYSTEM_READY,
    WAIT_CARD,
    CARD_STATUS
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    setContentView(activityCardReaderBinding.root)
    @Suppress("DEPRECATION")
    progress = ProgressDialog(this)
    @Suppress("DEPRECATION") progress.setMessage(getString(R.string.please_wait))
    progress.setCancelable(false)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(menuItem)
  }

  override fun onResume() {
    super.onResume()
    if (AppSettings.readerType == ReaderType.FLOWBIRD) {
      activityCardReaderBinding.animation.repeatCount = 0
    }
    activityCardReaderBinding.animation.playAnimation()

    if (!ticketingService.readersInitialized) {
      GlobalScope.launch {
        withContext(Dispatchers.Main) { showProgress() }
        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            ticketingService.init(cardReaderObserver, this@ReaderActivity, AppSettings.readerType)
            handleAppEvents(AppState.WAIT_CARD, null)
            ticketingService.startNfcDetection()
          } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (ticketingService.readersInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      ticketingService.startNfcDetection()
    }
    if (AppSettings.batteryPowered) {
      timer = Timer() // Need to reinit timer after cancel
      timer.schedule(
          object : TimerTask() {
            override fun run() {
              runOnUiThread { onBackPressed() }
            }
          },
          RETURN_DELAY_MS.toLong())
    }
  }

  override fun onPause() {
    super.onPause()
    activityCardReaderBinding.animation.cancelAnimation()
    timer.cancel()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }

  override fun onDestroy() {
    ticketingService.onDestroy(cardReaderObserver)
    cardReaderObserver = null
    super.onDestroy()
  }

  /**
   * main app state machine handle
   *
   * @param appState
   * @param readerEvent
   */
  private fun handleAppEvents(appState: AppState, readerEvent: CardReaderEvent?) {
    var newAppState = appState
    Timber.i(
        "Current state = $currentAppState, wanted new state = $newAppState, event = ${readerEvent?.type}")
    when (readerEvent?.type) {
      CardReaderEvent.Type.CARD_INSERTED,
      CardReaderEvent.Type.CARD_MATCHED -> {
        if (newAppState == AppState.WAIT_SYSTEM_READY) {
          return
        }
        Timber.i("Process the selection result...")
        val error =
            ticketingService.analyseSelectionResult(readerEvent.scheduledCardSelectionsResponse)
        if (error != null) {
          Timber.e("Card not selected: %s", error)
          ticketingService.displayResultFailed()
          changeDisplay(
              CardReaderResponse(
                  status = Status.INVALID_CARD,
                  cardType = "Unknown card type",
                  contract = null,
                  validation = null,
                  errorMessage = error))
          return
        }
        Timber.i("A Calypso Card selection succeeded.")
        newAppState = AppState.CARD_STATUS
      }
      CardReaderEvent.Type.CARD_REMOVED -> {
        currentAppState = AppState.WAIT_SYSTEM_READY
      }
      else -> {
        Timber.w("Event type not handled.")
      }
    }
    when (newAppState) {
      AppState.WAIT_SYSTEM_READY,
      AppState.WAIT_CARD -> {
        currentAppState = newAppState
      }
      AppState.CARD_STATUS -> {
        currentAppState = newAppState
        when (readerEvent?.type) {
          CardReaderEvent.Type.CARD_INSERTED,
          CardReaderEvent.Type.CARD_MATCHED -> {
            GlobalScope.launch {
              try {
                withContext(Dispatchers.Main) { progress.show() }
                val validationResult =
                    withContext(Dispatchers.IO) {
                      ticketingService.executeValidationProcedure(
                          this@ReaderActivity, locationRepository.getLocations())
                    }
                withContext(Dispatchers.Main) {
                  progress.dismiss()
                  changeDisplay(validationResult)
                }
              } catch (e: IllegalStateException) {
                Timber.e(e)
                Timber.e("Load ERROR page after exception = ${e.message}")
                changeDisplay(
                    CardReaderResponse(
                        status = Status.ERROR,
                        cardType = "Unknown card type",
                        nbTicketsLeft = 0,
                        contract = "",
                        validation = null,
                        errorMessage = e.message))
              }
            }
          }
          else -> {
            // Do nothing
          }
        }
      }
    }
    Timber.i("New state = $currentAppState")
  }

  private fun changeDisplay(cardReaderResponse: CardReaderResponse?) {
    if (cardReaderResponse != null) {
      if (cardReaderResponse.status === Status.LOADING) {
        activityCardReaderBinding.presentCardTv.visibility = View.GONE
        activityCardReaderBinding.mainView.setBackgroundColor(
            ContextCompat.getColor(this, R.color.turquoise))
        supportActionBar?.show()
        if (AppSettings.readerType == ReaderType.FLOWBIRD) {
          activityCardReaderBinding.animation.repeatCount = 0
        } else {
          activityCardReaderBinding.animation.repeatCount = LottieDrawable.INFINITE
        }
        activityCardReaderBinding.animation.playAnimation()
      } else {
        runOnUiThread { activityCardReaderBinding.animation.cancelAnimation() }
        val intent = Intent(this, CardSummaryActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(CardReaderResponse::class.simpleName, cardReaderResponse)
        intent.putExtra(Bundle::class.java.simpleName, bundle)
        startActivity(intent)
      }
    } else {
      activityCardReaderBinding.presentCardTv.visibility = View.VISIBLE
    }
  }

  private fun showNoProxyReaderDialog(t: Throwable) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.error_title)
    builder.setMessage(t.message)
    builder.setNegativeButton(R.string.quit) { _, _ -> finish() }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.show()
  }

  private fun showProgress() {
    if (!progress.isShowing) {
      progress.show()
    }
  }

  private fun dismissProgress() {
    if (progress.isShowing) {
      progress.dismiss()
    }
  }

  companion object {
    private const val RETURN_DELAY_MS = 30000
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      Timber.i("New ReaderEvent received:${readerEvent?.type?.name}")
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
