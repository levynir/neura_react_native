Nerua's package name is neruareactnative

My package and application name is nerua_react_native

Changes already done:
<ol>
<li>Changed calles to R to my package name
<li>In SDKManagerModule changed .getIdentifier calls to .identifier calls. Same for all AppSubscription properties.
<li>Added secrets in res/strings
</ol>

The module files do not compile. If I remove then the app compile but crash on startup on android emulator.
