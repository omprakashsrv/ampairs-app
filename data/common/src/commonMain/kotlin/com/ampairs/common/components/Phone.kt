package com.ampairs.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.phone.PhoneNumberValidator
import org.jetbrains.compose.resources.stringResource
import ampairsapp.data.common.generated.resources.Res
import ampairsapp.data.common.generated.resources.phone_field_label
import ampairsapp.data.common.generated.resources.phone_code_label

@Composable
fun Phone(
    countryCode: Int,
    phone: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit,
    onValidChange: (Boolean) -> Unit,
) {
    var phoneNumber by remember(phone) { mutableStateOf(phone) }
    var countryCodeText by remember(countryCode) { mutableStateOf("+ $countryCode") }
    var valid by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            modifier = Modifier.width(100.dp),
            value = countryCodeText,
            onValueChange = { },
            readOnly = true,
            maxLines = 1,
            label = { Text(stringResource(Res.string.phone_code_label)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = phoneNumber,
            onValueChange = { input ->
                phoneNumber = input.filter { it.isDigit() }
                if (phoneNumber.isEmpty()) {
                    valid = true
                } else {
                    try {
                        val result = PhoneNumberValidator().validate(phoneNumber)
                        valid = result is ValidationResult.Valid
                    } catch (_: Exception) {
                        valid = false
                    }
                }
                onValueChange(phoneNumber)
                onValidChange(valid)
            },
            readOnly = readOnly,
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            isError = !valid,
            label = { Text(stringResource(Res.string.phone_field_label)) },
        )
    }
}
