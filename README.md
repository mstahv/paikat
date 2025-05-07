# Paik.at

This is a WIP demo/test app that implements a mobile app for hunting teams 
(although with different terminology the app could fit for various other tasks as well 🤷‍♂️).

Features:

 * New user registration
 * Passkey registration, preferred mapstyle selection etc on Profile page
 * Team assembly and sending invites (with OTT) using the [share API](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share) 
 * Spot planning (both map centric view and a more traditional "CRUD")
 * Manual spot assignment and a "spot raffle"
 * Web Push Notifications for team members when they should move to their spots and when the actual session starts
 * Map and info view for session time usage for usage during a session. Geolocation API is utilized to share progress and locations within the team.
 * Pretty much everything **written in 100% pure Java**

A (non-complete) list of technologies utilized:

 * Vaadin
 * EclipseStore
 * Spring Boot
 * Spring Security with WebAuthn, OTT
 * MapLibre (vector tile maps FTW) in pure Java using a Vaadin add-on
 * Modern browser features (via Java abstraction): PWA, WebSockets, Web Push Notification, Geolocation, Webauthn (~ passkeys), Share, Web Components and probably lot more that is forgotten.

Import the project to the IDE of your choosing as a Maven project. 

Run application locally by directly running `DevModeApplication` (in src/test/java) class from your IDE or `mvn spring-boot:test-run`. This starts Vaadin in development mode.

Open http://localhost:8080/ in browser.

For actual deployment, you can use e.g. Spring Boot Maven plugin to build docker image and deploy that. Note that the architecture is not designed for horizontal scaling as such. EclipseStore usage would need to be implemented in a different manner.

For more information on Vaadin Flow, visit https://vaadin.com/flow.

## Notes for developers/testers/tire-kickers

If you want to test Web Push Notifications, you need to submit "VAPID keys" and configure EclipseStore a bit. Without them the app ought to start fine, but notifications just don't work.

Subscriptions are saved in EclipseStore backend as records (coming from Vaadin Flow), thus record support needs this (or similar explicit parameter to launch command in IDE):

    export JAVA_TOOL_OPTIONS="--add-exports=java.base/jdk.internal.misc=org.eclipse.serializer.base"

To generate VAPID keys, use e.g. npm:

    npx web-push generate-vapid-keys

Then expose these environment variables (or use put them to application.properties with `webpush.publickey` syntax):

    export WEBPUSH_PUBLICKEY=BJpgYaanSUGyu9kqpCZEXcyUoZ6XMYQ6s74G4MTDkbI6KGwQG1OdHqgSQD0ffQ4ITZsHFvwC3cyXosNfcfaiR9M 
    export WEBPUSH_PRIVATEKEY=Rv5iqzJVhEyYatsEdaLwrUn5RWVSwK7RDDzMakPcT2g
    export WEBPUSH_SUBJECT=mailto:your@email.com

Also, in case you plan to deploy your own instance of this app, you should override the random key used for stateless 
JWT authentication with your own random key (this sets the hardcoded key used for demo):

    export JWT_AUTH_SECRET=u30vWDWbkG/ZKtBeYipTD5tQ4Rwso8mRSpxemlFk3Cc=
