package com.example.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.Settings
import java.nio.charset.Charset

object NfcCardReaderHelper {

  data class NfcHardwareStatus(
    val isSupported: Boolean,
    val isEnabled: Boolean,
  )

  fun getHardwareStatus(context: Context): NfcHardwareStatus {
    val adapter = NfcAdapter.getDefaultAdapter(context)
    return NfcHardwareStatus(
      isSupported = adapter != null,
      isEnabled = adapter?.isEnabled == true,
    )
  }

  fun openNfcSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_NFC_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }
      .onFailure {
        val fallback =
          Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching { context.startActivity(fallback) }
      }
  }

  fun enableReaderMode(
    activity: Activity,
    onPayloadRead: (String) -> Unit,
    onError: (String) -> Unit,
  ): Boolean {
    val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
    if (!adapter.isEnabled) return false

    val flags =
      NfcAdapter.FLAG_READER_NFC_A or
        NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V

    val options = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }

    adapter.enableReaderMode(
      activity,
      { tag: Tag ->
        runCatching { extractTagPayload(tag) }
          .onSuccess { payload ->
            if (payload.isNotBlank()) {
              activity.runOnUiThread { onPayloadRead(payload) }
            } else {
              activity.runOnUiThread { onError("البطاقة فارغة ولا تحتوي على بيانات") }
            }
          }
          .onFailure { err ->
            activity.runOnUiThread {
              onError(err.message ?: "تعذّرت قراءة بطاقة NFC")
            }
          }
      },
      flags,
      options,
    )
    return true
  }

  fun disableReaderMode(activity: Activity) {
    runCatching { NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity) }
  }

  internal fun extractTagPayload(tag: Tag): String {
    val ndef = Ndef.get(tag)
    if (ndef != null) {
      val message: NdefMessage? =
        ndef.cachedNdefMessage
          ?: runCatching {
            ndef.connect()
            val msg = ndef.ndefMessage
            ndef.close()
            msg
          }
          .getOrNull()

      if (message != null) {
        for (record in message.records) {
          val decoded = decodeNdefRecord(record)
          if (!decoded.isNullOrBlank()) return decoded
        }
      }
    }
    return tag.id?.joinToString("") { "%02X".format(it) }.orEmpty()
  }

  internal fun decodeNdefRecord(record: NdefRecord): String? {
    return when {
      record.tnf == NdefRecord.TNF_WELL_KNOWN &&
        record.type.contentEquals(NdefRecord.RTD_TEXT) -> decodeTextRecord(record.payload)

      record.tnf == NdefRecord.TNF_WELL_KNOWN &&
        record.type.contentEquals(NdefRecord.RTD_URI) -> record.toUri()?.toString()

      record.tnf == NdefRecord.TNF_ABSOLUTE_URI -> String(record.payload, Charsets.UTF_8)

      record.tnf == NdefRecord.TNF_MIME_MEDIA -> String(record.payload, Charsets.UTF_8)

      else -> record.toUri()?.toString() ?: String(record.payload, Charsets.UTF_8).takeIf { it.isNotBlank() }
    }
  }

  private fun decodeTextRecord(payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val status = payload[0].toInt()
    val encoding: Charset = if ((status and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
    val langCodeLength = status and 0x3F
    if (1 + langCodeLength >= payload.size) return null
    return String(payload, 1 + langCodeLength, payload.size - 1 - langCodeLength, encoding).trim()
  }

  /**
   * يستخرج رقم الهاتف أو معرّف الزبون (CUS-XXX) من رابط بطاقة الولاء أو النص المخزّن في NFC.
   */
  fun parseCustomerIdentifier(rawPayload: String): String {
    val trimmed = rawPayload.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) ||
      trimmed.startsWith("https://", ignoreCase = true)
    ) {
      val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
      val phoneParam = uri?.getQueryParameter("phone")
      if (!phoneParam.isNullOrBlank()) return phoneParam.trim()
      val idParam = uri?.getQueryParameter("id") ?: uri?.getQueryParameter("customerId")
      if (!idParam.isNullOrBlank()) return idParam.trim()
    }
    val cusMatch = Regex("CUS-[A-Za-z0-9]+", RegexOption.IGNORE_CASE).find(trimmed)
    if (cusMatch != null) return cusMatch.value.uppercase()

    val phoneMatch = Regex("(?:\\+212|212|0)[5-7][0-9]{8}").find(trimmed.replace(" ", ""))
    if (phoneMatch != null) return phoneMatch.value

    return trimmed
  }
}
