package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.PaymentRepository;
import com.danir.libraryAPI.util.StripeConfig;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentIntent paymentIntentMock;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookService bookService;

    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private PaymentService paymentService;

    private Book book;
    private Person person;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setName("Test Book");
        book.setBorrowedDate(OffsetDateTime.now().minusDays(5));
        book.setIsDebtPaid(false);

        person = new Person();
        person.setFullName("John Doe");
        person.setBookList(List.of(book));
        Stripe.apiKey = "sk_test_dummy_key";
    }

    @Test
    void testCreatePaymentIntentForBook() throws StripeException {
        when(stripeConfig.getSecretKey()).thenReturn("sk_test_51QknoVPSnroF9soiVIiUry6UASUL5pZIm5nAFHaAIfA8yx8eemTRNOo8aobUWdYgj7sOKBvmnlpYavFwk4ozVGlV00fu6J7PYu");
        when(stripeConfig.getCurrency()).thenReturn("EUR");
        PaymentIntent mockIntent = mock(PaymentIntent.class);
      //  when(mockIntent.getClientSecret()).thenReturn("pk_test_51QknoVPSnroF9soiSVPaoSNMeNAt7EMakzF8evMmXIvf5IDgctYv5jj73yGCkPk3Il5sq8npOOCPrzDv7ubyv9B900tO99WUBh");

        assertDoesNotThrow(() -> paymentService.createPaymentIntentForBook(book));
    }

  //  @Test
//    void testConfirmPayment() throws StripeException {
//        // Мокаем метод, чтобы он не делал запросы в Stripe
//
//        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
//            mockedPaymentIntent.when(() -> PaymentIntent.retrieve("test_payment_id"))
//                    .thenReturn(paymentIntentMock);
//
//            when(paymentIntentMock.getStatus()).thenReturn("succeeded");
//
//            paymentService.verifyPayment("test_payment_id");
//
//            // Убедись, что метод действительно вызван
//            mockedPaymentIntent.verify(() -> PaymentIntent.retrieve("sk_test_dummy_key"));
//        }
//
//        assertDoesNotThrow(() -> paymentService.confirmPayment("sk_test_dummy_key", person, book));
//        assertTrue(book.getIsDebtPaid());
//    }


    @Test
    void testScheduledTask_CheckIfBookReturnedAfterPayment() {
        book.setPaymentDate(OffsetDateTime.now().minusHours(25));
        when(bookService.findAll()).thenReturn(List.of(book));

        paymentService.checkIfBookReturnedAfterPayment();
        assertFalse(book.getIsDebtPaid());
    }
}
