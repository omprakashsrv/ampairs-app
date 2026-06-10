package com.ampairs.form.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.validation.ValidationEngine

/**
 * Holds form values + per-field validation state for a [FormSchema]. Single-value fields store a
 * `String`; MULTI_CHOICE stores a `List<String>`. Values are keyed by `fieldKey` so the ViewModel can
 * map standard fields to entity columns and custom fields to the `attributes` map.
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
@Stable
class FormValueState internal constructor(
    private val schema: FormSchema,
    initialValues: Map<String, Any?>,
) {
    private val singleValues = mutableStateMapOf<String, String>()
    private val multiValues = mutableStateMapOf<String, List<String>>()
    private val errors = mutableStateMapOf<String, String>()

    init {
        schema.fields.forEach { field ->
            val initial = initialValues[field.fieldKey]
            if (field.dataType == FieldDataType.MULTI_CHOICE) {
                @Suppress("UNCHECKED_CAST")
                multiValues[field.fieldKey] = (initial as? List<String>) ?: emptyList()
            } else {
                singleValues[field.fieldKey] = initial?.toString() ?: field.defaultValue.orEmpty()
            }
        }
    }

    fun text(fieldKey: String): String = singleValues[fieldKey].orEmpty()

    fun selected(fieldKey: String): List<String> = multiValues[fieldKey].orEmpty()

    fun error(fieldKey: String): String? = errors[fieldKey]

    fun setText(field: FormField, value: String) {
        singleValues[field.fieldKey] = value
        revalidate(field)
    }

    fun setSelected(field: FormField, value: String) = setText(field, value)

    fun toggleMulti(field: FormField, value: String) {
        val current = multiValues[field.fieldKey].orEmpty()
        multiValues[field.fieldKey] = if (value in current) current - value else current + value
        revalidate(field)
    }

    private fun revalidate(field: FormField) {
        val msg = errorFor(field)
        if (msg == null) errors.remove(field.fieldKey) else errors[field.fieldKey] = msg
    }

    private fun errorFor(field: FormField): String? {
        return if (field.dataType == FieldDataType.MULTI_CHOICE) {
            val list = multiValues[field.fieldKey].orEmpty()
            ValidationEngine.firstError(list.joinToString(","), field.validationRules, isEmpty = list.isEmpty())
        } else {
            val v = singleValues[field.fieldKey].orEmpty()
            ValidationEngine.firstError(v, field.validationRules, isEmpty = v.isBlank())
        }
    }

    /** Validate every visible field; returns true when the form is valid. Populates inline errors. */
    fun validateAll(): Boolean {
        var ok = true
        schema.visibleFields().forEach { field ->
            val msg = errorFor(field)
            if (msg != null) {
                errors[field.fieldKey] = msg
                ok = false
            } else {
                errors.remove(field.fieldKey)
            }
        }
        return ok
    }

    /** Current values keyed by fieldKey (String for single, List<String> for multi) for persistence. */
    fun values(): Map<String, Any?> = buildMap {
        schema.fields.forEach { field ->
            if (field.dataType == FieldDataType.MULTI_CHOICE) put(field.fieldKey, multiValues[field.fieldKey].orEmpty())
            else put(field.fieldKey, singleValues[field.fieldKey].orEmpty())
        }
    }
}

/**
 * Remembers a [FormValueState] for the given schema; re-seeds when the schema identity or any of
 * [reseedKeys] change (e.g. an external import overwriting the backing form state).
 */
@Composable
fun rememberFormValueState(schema: FormSchema, initialValues: Map<String, Any?>, vararg reseedKeys: Any?): FormValueState =
    remember(schema, *reseedKeys) { FormValueState(schema, initialValues) }
