package com.ampairs.payment.ui

/**
 * True on the desktop (JVM) target. Desktop has a hardware keyboard, so the record-payment flow uses
 * a typed amount field + a mode dropdown there instead of the touch keypad + mode-tile grid.
 */
expect fun isDesktopPlatform(): Boolean
