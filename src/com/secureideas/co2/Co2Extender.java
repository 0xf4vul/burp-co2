/*
 * Copyright (c) 2014 Jason Gillam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.secureideas.co2;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionStateListener;
import com.secureideas.co2.cewler.CewlerTab;

import javax.swing.*;
import java.awt.*;

/**
 * Delegate fo burp.BurpExtender.  All the functionality should be implemented in this class rather than
 * burp.BurpExtender
 */
public class Co2Extender implements IBurpExtender, IExtensionStateListener {
    public static final String VERSION = "0.5.0";
    private Co2ConfigTab configTab;
    private IBurpExtenderCallbacks callbacks;
    private java.util.Timer co2Timer = new java.util.Timer("Co2",false);


    public Co2Extender() {
    }

    public IBurpExtenderCallbacks getCallbacks() {
        return callbacks;
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.setExtensionName("Burp Co2");

        IntruderPayloadProcessor payloadProcessor = new IntruderPayloadProcessor();
        callbacks.registerIntruderPayloadProcessor(payloadProcessor);

        MessageBeautifierFactory beautifier = new MessageBeautifierFactory(callbacks);

        SQLMapper mapper = new SQLMapper(callbacks.getHelpers(), this);
        callbacks.registerContextMenuFactory(mapper);

        //Hunter hunter = new Hunter(callbacks);

        UserGenerator userGenerator = new UserGenerator(callbacks);

        OAutherTab oauther = new OAutherTab(callbacks);

        NameManglerTab nameMangler = new NameManglerTab(callbacks);

        CewlerTab cewler = new CewlerTab(callbacks);

        final About about = new About(callbacks);
        co2Timer.schedule(new java.util.TimerTask(){
            @Override
            public void run() {
                about.performUpdateCheck();
            }
        }, 1000 * 10, 1000 * 60 * 60 * 24);  // check 10 seconds after startup + every 24 hrs

        Co2Configurable[] configurables = {mapper, userGenerator, nameMangler, cewler, oauther, payloadProcessor, beautifier, about};

        configTab = new Co2ConfigTab(callbacks, configurables);
        callbacks.customizeUiComponent(configTab);
        callbacks.addSuiteTab(configTab);

        callbacks.printOutput("Co2 Loaded.  Version: "+VERSION+" (build "+about.build+")");

    }

    @Override
    public void extensionUnloaded() {
        if(co2Timer!=null){
            co2Timer.cancel();
        }
    }

    /**
     * Callback to select the specified configurable item's tab.
     *
     * @param configurable The configurable item for which a tab should be selected.
     */
    public void selectConfigurableTab(Co2Configurable configurable) {
        Component tabComponent = configurable.getTabComponent();
        if (tabComponent != null) {
            Container parent = tabComponent.getParent();
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane) parent).setSelectedComponent(tabComponent);
            }

            Component mainCo2Tab = configTab.getUiComponent();
            if (mainCo2Tab != null) {
                Container mainParent = mainCo2Tab.getParent();
                if (mainParent instanceof JTabbedPane) {
                    ((JTabbedPane) mainParent).setSelectedComponent(mainCo2Tab);
                }
            }
        }
    }
}