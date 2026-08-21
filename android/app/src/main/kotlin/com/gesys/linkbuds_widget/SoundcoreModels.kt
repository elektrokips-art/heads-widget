package com.gesys.linkbuds_widget

import java.util.Locale
import java.util.UUID

/**
 * Per-model Soundcore RFCOMM service UUIDs -- each model's UUID is
 * `0CF12D31-FAC3-4553-BD80-D6832E7BXXXX`, where XXXX is the last 4 digits of Anker's internal
 * model code (confirmed against two already-verified entries: model a3947 = "Liberty 4 NC" =
 * UUID suffix 3947, model a3952 = "Liberty 3 Pro" = suffix 3952). This full list -- covering
 * every earbuds/headphones/speaker model in the catalog, not just the ones already wired up --
 * was extracted directly from the official Soundcore Android app's resources
 * (soundcore-6-4-0-17.apk, res/values/strings.xml, product_a* / label_a* string entries),
 * decompiled for interoperability purposes (same as reading Gadgetbridge/OpenPods/CoreSound
 * elsewhere in this app). Keys are lowercased, brand-prefix-stripped fragments matched against
 * the device's Bluetooth name; see [uuidForDeviceName] for how overlapping fragments (e.g.
 * "liberty 4" vs "liberty 4 nc" vs "liberty 4 pro") are disambiguated.
 */
object SoundcoreModels {
    private val FRAGMENT_TO_SUFFIX: Map<String, String> = mapOf(
        "soundcore 3" to "3117",
        "a20i" to "3948",
        "a30i" to "3958",
        "aeroclip" to "3388",
        "aerofit" to "3872",
        "aerofit 2" to "3874",
        "aerofit 2 pro" to "3875",
        "aerofit pro" to "3871",
        "boom 2" to "3138",
        "boom 2 plus" to "3134",
        "boom 2 se" to "3148",
        "boost" to "3145",
        "c30i" to "3330",
        "c40i" to "3331",
        "flare" to "3161",
        "flare +" to "3162",
        "flare 2" to "3165",
        "flare mini" to "3167",
        "flare s+" to "3163",
        "frames" to "3600",
        "glow" to "3166",
        "glow mini" to "3136",
        "h30i" to "3012",
        "icon+" to "3123",
        "infini pro" to "3372",
        "k20i" to "3994",
        "liberty 2" to "3913",
        "liberty 2 pro" to "3909",
        "liberty 2 pro+" to "3930",
        "liberty 3 pro" to "3952",
        "liberty 4" to "3953",
        "liberty 4 nc" to "3947",
        "liberty 4 pro" to "3954",
        "liberty 5" to "3957",
        "liberty 5 pro max" to "3956",
        "liberty air 2 pro" to "3951",
        "liberty air2" to "3910",
        "liberty neo 2" to "3926",
        "life 2 neo" to "3033",
        "life a1" to "3927",
        "life a2 nc" to "3935",
        "life a3i" to "3992",
        "life dot 2 nc" to "3931",
        "life dot 3i" to "3982",
        "life nc" to "3201",
        "life note 3" to "3933",
        "life note 3i" to "3983",
        "life note 3s" to "3945",
        "life note c" to "3943",
        "life p2 mini" to "3944",
        "life p3" to "3939",
        "life p3i" to "3993",
        "life q20" to "3025",
        "life q20+" to "3045",
        "life q30" to "3028",
        "life q35" to "3027",
        "life tune" to "3029",
        "life tune pro" to "3030",
        "mega" to "3392",
        "mini 3" to "3119",
        "mini 3 pro" to "3127",
        "motion 100" to "3133",
        "motion 300" to "3135",
        "motion boom" to "3118",
        "motion boom plus" to "3129",
        "motion x500" to "3131",
        "motion x600" to "3130",
        "motion+" to "3116",
        "p20i" to "3949",
        "p30i" to "3959",
        "p40i" to "3955",
        "p41i" to "3937",
        "powerconf" to "3301",
        "powerconf s3" to "3302",
        "q11i" to "3005",
        "q20i" to "3004",
        "r30" to "3398",
        "r50i vi" to "3969",
        "rave" to "3391",
        "rave mini" to "3390",
        "rave neo" to "3395",
        "rave party 2" to "3399",
        "select 2" to "3125",
        "select 2s" to "3171",
        "select 3" to "3172",
        "select pro" to "3126",
        "sleep a10" to "6610",
        "sleep a20" to "6611",
        "space a40" to "3936",
        "space one" to "3035",
        "space one pro" to "3062",
        "space q45" to "3040",
        "sport x10" to "3961",
        "sport x20" to "3968",
        "trance" to "3393",
        "trance go" to "3396",
        "v20i" to "3876",
        "v30i" to "3873",
        "v40i" to "3878",
        "vr p10" to "3850",
        "wakey" to "3300",
    )

    /**
     * Null if [deviceName] doesn't match any known Soundcore model. When multiple fragments
     * match (e.g. a device named "Liberty 4 NC" contains both "liberty 4" and "liberty 4 nc"),
     * the longest/most specific fragment wins.
     */
    fun uuidForDeviceName(deviceName: String): UUID? {
        val lower = deviceName.lowercase(Locale.ROOT)
        val suffix = FRAGMENT_TO_SUFFIX.entries
            .filter { (fragment, _) -> lower.contains(fragment) }
            .maxByOrNull { (fragment, _) -> fragment.length }
            ?.value
            ?: return null
        return UUID.fromString("0CF12D31-FAC3-4553-BD80-D6832E7B$suffix")
    }
}
