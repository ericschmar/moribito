Kotlin Multiplatform Licensing System
Architecture: Stripe + Cloudflare Workers + Keygen (or DIY)

1. Environment Secrets
   Set these in your Cloudflare Worker dashboard:

STRIPE_API_KEY: Your Stripe Secret Key.

STRIPE_WEBHOOK_SECRET: The secret from the Webhook dashboard.

RESEND_API_KEY: API Key from Resend.com.

PRIVATE_KEY_RAW: The hex string of your Ed25519 Private Key.

2. Cloudflare Worker Code
   JavaScript

import Stripe from 'stripe';

export default {
async fetch(request, env) {
const stripe = new Stripe(env.STRIPE_API_KEY, {
httpClient: Stripe.createFetchHttpClient(),
});

    const signature = request.headers.get('stripe-signature');
    const body = await request.text();

    try {
      const event = stripe.webhooks.constructEvent(body, signature, env.STRIPE_WEBHOOK_SECRET);

      if (event.type === 'checkout.session.completed') {
        const session = event.data.object;
        const email = session.customer_details.email;
        
        // Generate License
        const licenseKey = await generateSignedKey(email, env.PRIVATE_KEY_RAW);

        // Store for future migration
        await env.LICENSE_STORAGE.put(`license:${email}`, JSON.stringify({ key: licenseKey }));

        // Send via Resend
        await fetch("https://api.resend.com/emails", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${env.RESEND_API_KEY}`
          },
          body: JSON.stringify({
            from: "Licensing <licensing@yourdomain.com>",
            to: [email],
            subject: "Your App License Key",
            html: `<p>Your key: <code>${licenseKey}</code></p>`
          })
        });
      }
      return new Response("OK", { status: 200 });
    } catch (err) {
      return new Response(err.message, { status: 400 });
    }
}
};

async function generateSignedKey(email, privateKeyHex) {
const payload = `${email}|2099-12-31`;
const privKeyBuffer = new Uint8Array(privateKeyHex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
const privateKey = await crypto.subtle.importKey("raw", privKeyBuffer, "Ed25519", false, ["sign"]);
const sig = await crypto.subtle.sign("Ed25519", privateKey, new TextEncoder().encode(payload));
return `${btoa(payload)}.${btoa(String.fromCharCode(...new Uint8Array(sig)))}`.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
3. Kotlin Verification (JVM/Desktop)
   Kotlin

object LicenseVerifier {
private const val PUBLIC_KEY_B64 = "YOUR_PUBLIC_KEY_HERE"

    fun verify(licenseKey: String): Boolean {
        return try {
            val parts = licenseKey.split(".")
            val data = Base64.getDecoder().decode(parts[0])
            val sig = Base64.getDecoder().decode(parts[1])
            
            val pubKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_B64)))

            Signature.getInstance("Ed25519").apply {
                initVerify(pubKey)
                update(data)
            }.verify(sig)
        } catch (e: Exception) { false }
    }
}