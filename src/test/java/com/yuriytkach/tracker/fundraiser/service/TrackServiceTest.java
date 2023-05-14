package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.CommandFormParams;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {
  private static final Fund FUND_1 = Fund.builder()
          .id("1")
          .name("car")
          .currency(Currency.EUR)
          .goal(3000)
          .raised(1728)
          .owner("somePerson")
          .createdAt(Instant.now())
          .description("someDesc")
          .color("cyan")
          .build();

  @Mock
  private DonationStorageClient donationStorageClient;

  @Mock
  private FundService fundService;

  @Mock
  private ForexService forexService;

  @InjectMocks
  private TrackService tested;

  @BeforeEach
  public void setup() {
    tested.initCommandProcessors();
  }

  @Test
  void processUpdateFundCommandOk() {
    final CommandFormParams commandFormParams = new CommandFormParams();
    commandFormParams.setText("update car curr:usd goal:4250 desc:/Banderomobil/ color:blue");
    commandFormParams.setUserId("somePerson");

    when(fundService.findByNameOrException("car")).thenReturn(FUND_1);
    when(forexService.convertCurrency(anyInt(), eq(Currency.EUR), eq(Currency.USD)))
            .thenReturn((int) (FUND_1.getRaised() * 1.1));
    final var capture = ArgumentCaptor.forClass(Fund.class);
    doNothing().when(fundService).updateFund(capture.capture());

    final SlackResponse response = tested.process(commandFormParams);

    verify(fundService).updateFund(capture.capture());

    assertThat(response.getResponseType()).isEqualTo(SlackResponse.RESPONSE_PRIVATE);
    assertThat(capture.getValue().getCurrency()).isEqualTo(Currency.USD);
    assertThat(capture.getValue().getRaised()).isEqualTo((int) (FUND_1.getRaised() * 1.1));
    assertThat(response.getText())
            .isEqualTo(":white_check_mark: " + "The fund with name: `car` has been updated successfully!");
  }

  @Test
  void processUpdateFundCommandWrongUser() {
    final CommandFormParams commandFormParams = new CommandFormParams();
    commandFormParams.setText("update car curr:usd goal:4250 desc:/Banderomobil/ color:blue");
    commandFormParams.setUserId("anotherPerson");

    final String expectedExceptionMessage = ":x: Fund `car` owned by `somePerson`: Can't update fund";
    when(fundService.findByNameOrException("car")).thenReturn(FUND_1);

    final SlackResponse response = tested.process(commandFormParams);

    assertThat(response.getResponseType()).isEqualTo(SlackResponse.RESPONSE_PRIVATE);
    assertThat(response.getText()).isEqualTo(expectedExceptionMessage);
  }
}
