package com.yuriytkach.tracker.fundraiser.mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.mono.model.MonobankStatement;
import com.yuriytkach.tracker.fundraiser.service.DonationTracker;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.PersonNameConverter;

@DisplayName("MonobankStatementProcessor tests for failed input data")
@ExtendWith(MockitoExtension.class)
class MonobankStatementProcessorFailureTest {

  private static final int POSITIVE_AMOUNT = 10;
  private static final String ACCOUNT_ID = "accountId";

  @Mock
  private FundService fundService;
  @Mock
  private DonationTracker donationTracker;
  @Mock
  private PersonNameConverter nameConverter;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MonobankStatement statement;

  @InjectMocks
  private MonobankStatementProcessor tested;

  @Test
  void shouldDoNothingIfNegativeAmount() {
    when(statement.getData().getStatementItem().getAmount()).thenReturn(-POSITIVE_AMOUNT);

    tested.processStatement(statement);

    verifyNoInteractions(fundService, donationTracker, nameConverter);
  }

  @Test
  void shouldDoNothingIfCurrencyNotFound() {
    when(statement.getData().getStatementItem().getAmount()).thenReturn(POSITIVE_AMOUNT);
    when(statement.getData().getStatementItem().getCurrencyCode()).thenReturn(0);

    tested.processStatement(statement);

    verifyNoInteractions(fundService, donationTracker, nameConverter);
  }

  @Test
  void shouldDoNothingIfFundIsNotFound() {
    when(statement.getData().getStatementItem().getAmount()).thenReturn(POSITIVE_AMOUNT);
    when(statement.getData().getStatementItem().getCurrencyCode()).thenReturn(Currency.UAH.getIsoCode());
    when(statement.getData().getAccount()).thenReturn(ACCOUNT_ID);

    when(fundService.findEnabledByMonoAccount(any())).thenReturn(Optional.empty());

    tested.processStatement(statement);

    verifyNoInteractions(donationTracker, nameConverter);

    verify(fundService).findEnabledByMonoAccount(ACCOUNT_ID);
  }
}
