package io.blockwavelabs.tree.dto.token;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendTrxStatusToggleReqDto {
    private Long sendTrxIndex;
    private Boolean isValid;

}
