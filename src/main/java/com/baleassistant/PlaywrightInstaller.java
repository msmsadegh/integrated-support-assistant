
package com.baleassistant;

import com.microsoft.playwright.CLI;

public class PlaywrightInstaller {

    public static void main(String[] args) throws Exception {

        System.out.println("Installing Playwright Chromium...");

        CLI.main(new String[]{"install", "chromium"});

        System.out.println("Chromium installation completed.");
    }
}
