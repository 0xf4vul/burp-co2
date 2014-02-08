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

import javax.swing.*;
import java.awt.*;

public class Co2Extender implements IBurpExtender {
    public static final String VERSION = "0.4";
    private Co2ConfigTab configTab;
    private IBurpExtenderCallbacks callbacks;


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

        Hunter hunter = new Hunter(callbacks);

        Lister lister = new Lister(callbacks);

        OAutherTab oauther = new OAutherTab(callbacks);

        About about = new About();

        Co2Configurable[] configurables = {mapper,  lister, oauther, payloadProcessor, beautifier, about};

        configTab = new Co2ConfigTab(callbacks, configurables);
        callbacks.customizeUiComponent(configTab);
        callbacks.addSuiteTab(configTab);

        callbacks.printOutput("Co2 Loaded.  Version: "+VERSION+" (build "+about.build+")");
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