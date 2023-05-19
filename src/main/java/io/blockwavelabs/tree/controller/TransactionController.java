package io.blockwavelabs.tree.controller;

import io.blockwavelabs.tree.domain.tokenSendTrx.TokenSendTransaction;
import io.blockwavelabs.tree.dto.token.CreateSendTokenTrxDto;
import io.blockwavelabs.tree.dto.token.ReceiveSendTokenTrxReqDto;
import io.blockwavelabs.tree.dto.token.SendTrxStatusToggleReqDto;
import io.blockwavelabs.tree.dto.token.TokenSendTrxInfoDto;
import io.blockwavelabs.tree.dto.utils.ResultDto;
import io.blockwavelabs.tree.service.TokenSendTransactionService;
import io.blockwavelabs.tree.service.UserService;
import io.blockwavelabs.tree.service.userdetails.UserAdapter;
import kotlin.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
public class TransactionController {
    private final TokenSendTransactionService tokenSendTrxService;
    private final UserService userService;

    @GetMapping("public/trxs")
    public ResponseEntity<ResultDto<TokenSendTrxInfoDto>> getTokenSendTrxFromLinkKey(@RequestParam(value="link_key") String linkKey){
        Long tokenSendTrxIndex = tokenSendTrxService.getTokenSendTrxIndexByLinkKey(linkKey);
        TokenSendTrxInfoDto tokenSendTrxInfoDto = tokenSendTrxService.getTokenSendTrxInfoDto(tokenSendTrxIndex);
        return ResponseEntity.ok().body(ResultDto.res(HttpStatus.OK, HttpStatus.OK.toString(), tokenSendTrxInfoDto));
    }

    @PatchMapping("trxs/toggle/valid")
    public ResponseEntity<ResultDto<TokenSendTrxInfoDto>> toggleValid(@AuthenticationPrincipal UserAdapter userAdapter, @RequestBody SendTrxStatusToggleReqDto sendTrxStatusToggleReqDto){
        TokenSendTrxInfoDto tokenSendTrxInfoDto = tokenSendTrxService.toggleValid(userAdapter.getUser(), sendTrxStatusToggleReqDto.getSendTrxIndex(), sendTrxStatusToggleReqDto.getIsValid());
        return ResponseEntity.ok().body(ResultDto.res(HttpStatus.OK, HttpStatus.OK.toString(), tokenSendTrxInfoDto));
    }

    @PostMapping("trxs/send/receive")
    public ResponseEntity<ResultDto<TokenSendTrxInfoDto>> receiveTokenSend(@AuthenticationPrincipal UserAdapter userAdapter, @RequestBody ReceiveSendTokenTrxReqDto receiveSendTokenTrxReqDto, @RequestParam(value="trx_index") Long trxIndex){
        TokenSendTrxInfoDto tokenSendTrxInfoDto = tokenSendTrxService.receiveTokenSend(userAdapter.getUser(), trxIndex, receiveSendTokenTrxReqDto.getReceiveTokenWalletType(), receiveSendTokenTrxReqDto.getReceiverWalletAddress(), receiveSendTokenTrxReqDto.getTransactionGasFee());
        return ResponseEntity.ok().body(ResultDto.res(HttpStatus.OK, HttpStatus.OK.toString(), tokenSendTrxInfoDto));
    }

    @PostMapping("trxs/send")
    public ResponseEntity<ResultDto<TokenSendTrxInfoDto>> createTokenSendLink(@AuthenticationPrincipal UserAdapter userAdapter, @RequestBody CreateSendTokenTrxDto createSendTokenTrxDto){
        TokenSendTrxInfoDto tokenSendTrxInfoDto = tokenSendTrxService.createTokenSendLink(userAdapter.getUser(), createSendTokenTrxDto);
        return ResponseEntity.ok().body(ResultDto.res(HttpStatus.OK, HttpStatus.OK.toString(), tokenSendTrxInfoDto));
    }
}
