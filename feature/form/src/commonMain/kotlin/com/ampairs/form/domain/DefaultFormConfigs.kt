package com.ampairs.form.domain

/**
 * Default form configurations for entities
 *
 * ⚠️ FOR BACKEND REFERENCE ONLY ⚠️
 *
 * This file serves as documentation and reference for backend developers
 * to seed default form configurations in the database.
 *
 * The backend should automatically seed these configurations when:
 * - GET /api/v1/form/schema?entity_type=customer is called
 * - No configuration exists for that entity type
 * - Return the seeded configuration to the client
 *
 * This prevents duplicate configurations when multiple users access
 * the system for the first time.
 *
 * NOTE: Functions in this object are intentionally unused in mobile code.
 * They exist purely as reference documentation for backend implementation.
 */
object DefaultFormConfigs {

    /**
     * Default customer form field configurations
     * Matches the static fields in CustomerFormScreen
     *
     * BACKEND REFERENCE: Use this structure when seeding customer form config
     */
    fun getDefaultCustomerFieldConfigs(): List<EntityFieldConfig> = listOf(
        // === Basic Information Section ===
        EntityFieldConfig(
            uid = "customer-field-name",
            entityType = "customer",
            fieldName = "name",
            displayName = "Customer Name",
            visible = true,
            mandatory = true,
            enabled = true,
            displayOrder = 1,
            placeholder = "Enter customer name",
            helpText = "Full name of the customer"
        ),
        EntityFieldConfig(
            uid = "customer-field-email",
            entityType = "customer",
            fieldName = "email",
            displayName = "Email",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 2,
            validationType = "email",
            placeholder = "customer@example.com"
        ),
        EntityFieldConfig(
            uid = "customer-field-customerType",
            entityType = "customer",
            fieldName = "customerType",
            displayName = "Customer Type",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 3,
            validationType = "select",
            helpText = "Type of customer (Retail, Wholesale, etc.)"
        ),
        EntityFieldConfig(
            uid = "customer-field-customerGroup",
            entityType = "customer",
            fieldName = "customerGroup",
            displayName = "Customer Group",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 4,
            validationType = "select",
            helpText = "Group classification for customer"
        ),
        EntityFieldConfig(
            uid = "customer-field-countryCode",
            entityType = "customer",
            fieldName = "countryCode",
            displayName = "Country Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 5,
            validationType = "number",
            defaultValue = "91",
            placeholder = "91",
            helpText = "International dialing code (e.g., 91 for India, 1 for USA)"
        ),
        EntityFieldConfig(
            uid = "customer-field-phone",
            entityType = "customer",
            fieldName = "phone",
            displayName = "Phone Number",
            visible = true,
            mandatory = true,
            enabled = true,
            displayOrder = 6,
            validationType = "phone",
            placeholder = "Enter phone number"
        ),
        EntityFieldConfig(
            uid = "customer-field-landline",
            entityType = "customer",
            fieldName = "landline",
            displayName = "Landline",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 7,
            placeholder = "Enter landline number",
            helpText = "Landline number with area code"
        ),

        // === Business Information Section ===
        EntityFieldConfig(
            uid = "customer-field-gstNumber",
            entityType = "customer",
            fieldName = "gstNumber",
            displayName = "GST Number",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 8,
            validationType = "gstin",
            placeholder = "Enter 15-digit GST number",
            helpText = "Goods and Services Tax Identification Number"
        ),
        EntityFieldConfig(
            uid = "customer-field-panNumber",
            entityType = "customer",
            fieldName = "panNumber",
            displayName = "PAN Number",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 9,
            validationType = "pan",
            placeholder = "Enter 10-digit PAN",
            helpText = "Permanent Account Number for tax purposes"
        ),

        // === Credit Management Section ===
        EntityFieldConfig(
            uid = "customer-field-creditLimit",
            entityType = "customer",
            fieldName = "creditLimit",
            displayName = "Credit Limit",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 10,
            validationType = "number",
            placeholder = "0.00",
            helpText = "Maximum credit amount allowed"
        ),
        EntityFieldConfig(
            uid = "customer-field-creditDays",
            entityType = "customer",
            fieldName = "creditDays",
            displayName = "Credit Days",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 11,
            validationType = "number",
            placeholder = "0",
            helpText = "Number of days for credit payment"
        ),

        // === Main Address Section ===
        EntityFieldConfig(
            uid = "customer-field-address",
            entityType = "customer",
            fieldName = "address",
            displayName = "Address",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 11,
            placeholder = "Enter address"
        ),
        EntityFieldConfig(
            uid = "customer-field-street",
            entityType = "customer",
            fieldName = "street",
            displayName = "Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 12,
            placeholder = "Enter street name"
        ),
        EntityFieldConfig(
            uid = "customer-field-street2",
            entityType = "customer",
            fieldName = "street2",
            displayName = "Street 2",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 13,
            placeholder = "Additional street information"
        ),
        EntityFieldConfig(
            uid = "customer-field-city",
            entityType = "customer",
            fieldName = "city",
            displayName = "City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 14,
            placeholder = "Enter city"
        ),
        EntityFieldConfig(
            uid = "customer-field-pincode",
            entityType = "customer",
            fieldName = "pincode",
            displayName = "PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 15,
            placeholder = "Enter PIN code"
        ),
        EntityFieldConfig(
            uid = "customer-field-state",
            entityType = "customer",
            fieldName = "state",
            displayName = "State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 16,
            placeholder = "Select state"
        ),
        EntityFieldConfig(
            uid = "customer-field-country",
            entityType = "customer",
            fieldName = "country",
            displayName = "Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 17,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Location Section ===
        EntityFieldConfig(
            uid = "customer-field-latitude",
            entityType = "customer",
            fieldName = "latitude",
            displayName = "Latitude",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 18,
            validationType = "number",
            helpText = "GPS latitude coordinate"
        ),
        EntityFieldConfig(
            uid = "customer-field-longitude",
            entityType = "customer",
            fieldName = "longitude",
            displayName = "Longitude",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 19,
            validationType = "number",
            helpText = "GPS longitude coordinate"
        ),

        // === Billing Address Section ===
        EntityFieldConfig(
            uid = "customer-field-billingStreet",
            entityType = "customer",
            fieldName = "billingStreet",
            displayName = "Billing Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 20,
            placeholder = "Enter billing street"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingCity",
            entityType = "customer",
            fieldName = "billingCity",
            displayName = "Billing City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 21,
            placeholder = "Enter billing city"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingPincode",
            entityType = "customer",
            fieldName = "billingPincode",
            displayName = "Billing PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 22,
            placeholder = "Enter billing PIN"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingState",
            entityType = "customer",
            fieldName = "billingState",
            displayName = "Billing State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 23,
            placeholder = "Select billing state"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingCountry",
            entityType = "customer",
            fieldName = "billingCountry",
            displayName = "Billing Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 24,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Shipping Address Section ===
        EntityFieldConfig(
            uid = "customer-field-shippingStreet",
            entityType = "customer",
            fieldName = "shippingStreet",
            displayName = "Shipping Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 25,
            placeholder = "Enter shipping street"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingCity",
            entityType = "customer",
            fieldName = "shippingCity",
            displayName = "Shipping City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 26,
            placeholder = "Enter shipping city"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingPincode",
            entityType = "customer",
            fieldName = "shippingPincode",
            displayName = "Shipping PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 27,
            placeholder = "Enter shipping PIN"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingState",
            entityType = "customer",
            fieldName = "shippingState",
            displayName = "Shipping State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 28,
            placeholder = "Select shipping state"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingCountry",
            entityType = "customer",
            fieldName = "shippingCountry",
            displayName = "Shipping Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 29,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Status Section ===
        EntityFieldConfig(
            uid = "customer-field-status",
            entityType = "customer",
            fieldName = "status",
            displayName = "Status",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 30,
            validationType = "select",
            defaultValue = "ACTIVE",
            helpText = "Customer status (Active, Inactive, Suspended)"
        ),

        // === Customer Images Section ===
        EntityFieldConfig(
            uid = "customer-field-customerImages",
            entityType = "customer",
            fieldName = "customerImages",
            displayName = "Customer Images",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 31,
            helpText = "Photo gallery for customer documentation (storefront, documents, etc.)"
        )
    )

    /**
     * Default customer attribute definitions
     * These are PRECONFIGURED custom fields - users can only enter values, not create keys
     *
     * IMPORTANT: These are NOT free-form key-value pairs!
     * - Admin defines attribute keys in backend configuration
     * - Users can only fill values for predefined attributes
     * - No "Add Attribute" button for random attributes
     *
     * BACKEND REFERENCE: Use this structure when seeding customer attribute definitions
     */
    fun getDefaultCustomerAttributeDefinitions(): List<EntityAttributeDefinition> = listOf(
        // Example: Industry attribute
        EntityAttributeDefinition(
            uid = "customer-attr-industry",
            entityType = "customer",
            attributeKey = "industry",
            displayName = "Industry",
            dataType = AttributeDataType.STRING,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 1,
            category = "Business",
            placeholder = "e.g., Retail, Manufacturing, Services",
            helpText = "Customer's industry sector"
        ),
        // Example: Annual Revenue attribute
        EntityAttributeDefinition(
            uid = "customer-attr-annual-revenue",
            entityType = "customer",
            attributeKey = "annualRevenue",
            displayName = "Annual Revenue",
            dataType = AttributeDataType.NUMBER,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 2,
            category = "Financial",
            placeholder = "0.00",
            helpText = "Estimated annual revenue"
        ),
        // Example: Company Size attribute
        EntityAttributeDefinition(
            uid = "customer-attr-company-size",
            entityType = "customer",
            attributeKey = "companySize",
            displayName = "Company Size",
            dataType = AttributeDataType.STRING,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 3,
            category = "Business",
            enumValues = listOf("1-10", "11-50", "51-200", "201-500", "500+"),
            placeholder = "Select company size",
            helpText = "Number of employees"
        ),
        // Example: Payment Terms attribute
        EntityAttributeDefinition(
            uid = "customer-attr-payment-terms",
            entityType = "customer",
            attributeKey = "paymentTerms",
            displayName = "Payment Terms",
            dataType = AttributeDataType.STRING,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 4,
            category = "Financial",
            placeholder = "e.g., Net 30, Net 60",
            helpText = "Preferred payment terms"
        ),
        // Example: Tax Exemption attribute
        EntityAttributeDefinition(
            uid = "customer-attr-tax-exempt",
            entityType = "customer",
            attributeKey = "taxExempt",
            displayName = "Tax Exempt",
            dataType = AttributeDataType.BOOLEAN,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 5,
            category = "Tax",
            helpText = "Is customer tax exempt?"
        ),
        // Example: Notes/Remarks attribute
        EntityAttributeDefinition(
            uid = "customer-attr-notes",
            entityType = "customer",
            attributeKey = "notes",
            displayName = "Additional Notes",
            dataType = AttributeDataType.STRING,
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 6,
            category = "General",
            placeholder = "Enter any additional information",
            helpText = "Additional remarks about the customer"
        )
    )
}
