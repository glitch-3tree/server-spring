package io.blockwavelabs.tree.dto.token;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReceiveSendTokenTrxReqDto {
    private String receiverWalletAddress;
    private String receiveTokenWalletType;
    private Float transactionGasFee;
}
