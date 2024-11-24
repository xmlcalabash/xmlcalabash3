package com.xmlcalabash.app

/**
 * Run the XML Calabash application with the specified arguments.
 *
 * This function simply passes the command line arguments to [XmlCalabashCli].
 */
class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            XmlCalabashCli.run(args)
        }
    }
}

