package com.yuriytkach.tracker.fundraiser.mono.model;

import lombok.Data;

/**
 * <pre>
 * {
 *     "type": "StatementItem",
 *     "data": {
 *         "account": "aaabbbccc",
 *         "statementItem": {
 *             "id": "_uEuv4rXVY9yyIRgedNSwFzJuSQ33dlRzIBi7vFS",
 *             "time": 1667731529,
 *             "description": "Від: 🐈",
 *             "comment": "M",
 *             "mcc": 4829,
 *             "originalMcc": 4829,
 *             "amount": 10000,
 *             "operationAmount": 10000,
 *             "currencyCode": 980,
 *             "commissionRate": 0,
 *             "cashbackAmount": 0,
 *             "balance": 2760000,
 *             "hold": false
 *         }
 *     }
 * }
 * </pre>
 */
@Data
public class MonobankStatement {
  private final String type;
  private final MonobankStatementData data;

  @Data
  public static class MonobankStatementData {
    private final String account;
    private final MonobankStatementItem statementItem;
  }

  @Data
  public static class MonobankStatementItem {
    private final String id;
    private final long time;
    private final String description;
    private final String comment;
    private final int mcc;
    private final int originalMcc;
    private final int amount;
    private final int operationAmount;
    private final int currencyCode;
    private final int commissionRate;
    private final int cashbackAmount;
    private final int balance;
    private final boolean hold;
  }
}
