package com.friendlywings.automation.pdf;

import com.friendlywings.automation.model.Booking;

import java.io.IOException;
import java.nio.file.Path;

public interface PdfGenerator<T extends Booking> {

    boolean supports(Booking booking);

    Path generate(T booking) throws IOException;
}
