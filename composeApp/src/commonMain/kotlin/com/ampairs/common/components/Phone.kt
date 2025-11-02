package com.ampairs.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.phone.PhoneNumberValidator
import com.ampairs.ui.theme.Dimensions
import com.ampairs.ui.theme.AmpairsTheme
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.code
import ampairsapp.composeapp.generated.resources.phone
import androidx.compose.material3.TextFieldDefaults

@Composable
fun Phone(
    countryCode: Int,
    phone: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit,
    onValidChange: (Boolean) -> Unit,
) {
    var phoneNumber by remember {
        mutableStateOf(phone)
    }
    var countryCodeText by remember {
        mutableStateOf("+ $countryCode")
    }
    var valid by remember {
        mutableStateOf(true)
    }
    val dimensions = Dimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        verticalAlignment = Alignment.Top
    ) {
        // Country Code Field (Read-only)
        OutlinedTextField(
            modifier = Modifier.width(100.dp),
            value = countryCodeText,
            onValueChange = { }, // No-op since it's read-only
            readOnly = true,
            maxLines = 1,
            label = { Text(stringResource(Res.string.code)) },
            singleLine = true
        )

        // Phone Number Field (Editable)
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = phoneNumber,
            onValueChange = { phone ->
                phoneNumber = phone.filter { it.isDigit() }
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
                onValueChange(phoneNumber) // Pass filtered phone number
                onValidChange(valid)
            },
            readOnly = readOnly,
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            isError = !valid,
            label = { Text(stringResource(Res.string.phone)) },
        )
    }
}

@Composable
fun PhonePreview() {
    AmpairsTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Phone(
                countryCode = 91,
                phone = "9876543210",
                onValueChange = { },
                onValidChange = { }
            )
        }
    }
}
