package com.nrknithin.tube.music.fragments.list.kiosk;

import android.os.Bundle;

import com.nrknithin.tube.music.error.ErrorInfo;
import com.nrknithin.tube.music.error.UserAction;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import com.nrknithin.tube.music.util.KioskTranslator;
import com.nrknithin.tube.music.util.YouTubeMusicHelper;

public class DefaultKioskFragment extends KioskFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (serviceId < 0) {
            updateSelectedDefaultKiosk();
        }
    }

    private void updateSelectedDefaultKiosk() {
        try {
            serviceId = ServiceList.YouTube.getServiceId();

            final KioskList kioskList = ServiceList.YouTube.getKioskList();
            kioskId = YouTubeMusicHelper.getDefaultKioskId(ServiceList.YouTube);
            url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).getUrl();

            kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, requireContext());
            name = kioskTranslatedName;

            currentInfo = null;
            currentNextPage = null;
        } catch (final ExtractionException e) {
            showError(new ErrorInfo(e, UserAction.REQUESTED_KIOSK,
                    "Loading default kiosk for selected service"));
        }
    }
}
