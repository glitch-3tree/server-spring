package io.blockwavelabs.tree.service;

import io.blockwavelabs.tree.domain.tokenRefund.RefundStatusEnum;
import io.blockwavelabs.tree.domain.tokenRefund.TokenRefund;
import io.blockwavelabs.tree.domain.tokenRefund.TokenRefundRepository;
import io.blockwavelabs.tree.domain.tokenSendTrx.TokenSendTransaction;
import io.blockwavelabs.tree.domain.tokenSendTrx.TokenSendTransactionRepository;
import io.blockwavelabs.tree.domain.user.User;
import io.blockwavelabs.tree.domain.wallet.WalletTypeEnum;
import io.blockwavelabs.tree.dto.token.CreateSendTokenTrxDto;
import io.blockwavelabs.tree.dto.token.TokenRefundInfoDto;
import io.blockwavelabs.tree.dto.token.TokenSendTrxInfoDto;
import io.blockwavelabs.tree.dto.utils.ResultDto;
import io.blockwavelabs.tree.exception.SamTreeException;
import io.blockwavelabs.tree.exception.type.TokenSendExceptionType;
import io.blockwavelabs.tree.utils.TokenUtils;
import io.blockwavelabs.tree.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TokenSendTransactionService {
    private final TokenSendTransactionRepository tokenSendTrxRepository;
    private final TokenRefundRepository tokenRefundRepository;
    private final Utils utils;
    private final TokenUtils tokenUtils;

    @Transactional(readOnly = true)
    public Long getTokenSendTrxIndexByLinkKey(String linkKey){
        Optional<TokenSendTransaction> findTokenSendTrx = tokenSendTrxRepository.findByLinkKey(linkKey);
        if(findTokenSendTrx.isEmpty()){
            throw new SamTreeException(TokenSendExceptionType.NOT_EXISTS_TXN);
        }
        return findTokenSendTrx.get().getId();
    }

    @Transactional(readOnly = true)
    public TokenSendTrxInfoDto getTokenSendTrxInfoDto(Long snedTrxIndex){
        Optional<TokenSendTransaction> findTokenSendTrx = tokenSendTrxRepository.findTokenSendTransactionById(snedTrxIndex);
        if(findTokenSendTrx.isEmpty()){
            throw new SamTreeException(TokenSendExceptionType.NOT_EXISTS_TXN);
        }
        TokenSendTransaction tokenSendTrx = findTokenSendTrx.get();
        return TokenSendTrxInfoDto.of(tokenSendTrx);
    }

    @Transactional
    public TokenSendTrxInfoDto toggleValid(User user, Long sendTrxIndex, boolean valid){
        Optional<TokenSendTransaction> findTokenSendTrx = tokenSendTrxRepository.findTokenSendTransactionById(sendTrxIndex);
        if(findTokenSendTrx.isEmpty()){
            throw new SamTreeException(TokenSendExceptionType.NOT_EXISTS_TXN);
        }
        TokenSendTransaction tokenSendTrx = findTokenSendTrx.get();
        if(!user.getSocialId().equals(tokenSendTrx.getReceiverSocialId())){
            throw new SamTreeException(TokenSendExceptionType.USER_NOT_MATCHED);
        }
        tokenSendTrxRepository.updateValid(sendTrxIndex, valid);
        Optional<TokenSendTransaction> responseTrx = tokenSendTrxRepository.findTokenSendTransactionById(sendTrxIndex);
        return TokenSendTrxInfoDto.of(responseTrx.get());
    }

    @Transactional
    public TokenSendTrxInfoDto receiveTokenSend(User user, Long sendTrxIndex, String receiveTokenWalletType, String receiverWalletAddress, Float transactionGasFee){
        Optional<TokenSendTransaction> findTokenSendTrx = tokenSendTrxRepository.findTokenSendTransactionById(sendTrxIndex);
        if(findTokenSendTrx.isEmpty()){
            throw new SamTreeException(TokenSendExceptionType.NOT_EXISTS_TXN);
        }
        TokenSendTransaction tokenSendTrx = findTokenSendTrx.get();
        if(!user.getSocialId().equals(tokenSendTrx.getReceiverSocialId())){
            throw new SamTreeException(TokenSendExceptionType.USER_NOT_MATCHED);
        }
        tokenSendTrxRepository.updateAfterReceive(sendTrxIndex, receiverWalletAddress, receiveTokenWalletType, transactionGasFee, user.getId(), user.getUserId());
        Optional<TokenSendTransaction> responseTrx = tokenSendTrxRepository.findTokenSendTransactionById(sendTrxIndex);
        return TokenSendTrxInfoDto.of(responseTrx.get());
    }

    @Transactional
    public TokenSendTrxInfoDto createTokenSendLink(User user, CreateSendTokenTrxDto createSendTokenTrxDto){

        String randomLink = "";
        do {
            randomLink = utils.createRandomString(10);
        }while(tokenSendTrxRepository.existsByLinkKey(randomLink));

        TokenSendTransaction tokenSendTransaction = tokenSendTrxRepository.save(TokenSendTransaction.builder()
                        .senderUser(user)
                        .senderUserId(user.getUserId())
                        .senderWalletAddress(createSendTokenTrxDto.getSenderWalletAddress())
                        .senderTokenWalletType(WalletTypeEnum.valueOf(createSendTokenTrxDto.getSenderTokenWalletType()))
                        .receiverSocialPlatform(createSendTokenTrxDto.getReceiverSocialPlatform())
                        .receiverSocialId(createSendTokenTrxDto.getReceiverSocialId())
                        .tokenUdenom(createSendTokenTrxDto.getTokenUdenom())
                        .tokenAmount(createSendTokenTrxDto.getTokenAmount())
                        .transactionHash(createSendTokenTrxDto.getTransactionHash())
                        .transactionEscrowId(createSendTokenTrxDto.getTransactionEscrowId())
                        .expiredAt(createSendTokenTrxDto.getExpiredAt())
                        .tokenContractAddress(createSendTokenTrxDto.getTokenContractAddress())
                        .networkId(createSendTokenTrxDto.getNetworkId())
                        .linkKey(randomLink)
                        .build()
        );

        return TokenSendTrxInfoDto.of(tokenSendTransaction);
    }

    @Transactional
    public List<TokenRefundInfoDto> refund() throws Exception {
        //refund 필요한 instance 다 불러오기
        List<TokenRefund> refundList = tokenRefundRepository.getRefundTrxCandidates("GoerliETH");
        // 3. refund 실행
        for(TokenRefund refundTrx:refundList){
            System.out.println();
            System.out.println(">> index: " + refundTrx.getId());
            // refund 실행
            String refundTrxHash = tokenUtils.goerliEthTransfer(refundTrx.getSenderWalletAddress(), refundTrx.getTokenAmount());
            if(refundTrxHash.equals("FAILED")){
                continue;
            }
            // pending 상태 대비를 위해 1분, 3분, 5분 뒤 체크해주기
            int checkCnt = 0;
            int waitingSec = 60;
            String refundStatus = "";
            while (checkCnt < 3 && !refundStatus.equals("SUCCESS")){
                try {
                    Thread.sleep(waitingSec * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refundStatus = tokenUtils.goerliEthTransferCheck(refundTrxHash);
                System.out.print("[STEP2] refund status (");
                System.out.print(checkCnt);
                System.out.println(") : " + refundStatus);

                checkCnt += 1;
                waitingSec += 120;
            }
            // status 값 저장
            tokenRefundRepository.changeRefundStatus(refundTrx.getId(), refundStatus);
            if(!refundStatus.equals("FAILED")){
                //hash 값 저장
                tokenRefundRepository.saveTokenRefundHash(refundTrx.getId(), refundTrxHash);
                if(refundStatus.equals("SUCCESS")){
                    //gas값 저장
                    System.out.println("Result: SUCCESS");
                    HashMap<String, Float> gasFeeMap = tokenUtils.getGasFee(refundTrxHash);
                    tokenRefundRepository.saveTokenRefundGasFee(refundTrx.getId(), gasFeeMap.get("transactionFeeInGwei"), gasFeeMap.get("gasPriceInWei"),gasFeeMap.get("gasLimitInWei"),gasFeeMap.get("gasUsedInWei"));
                }else{
                    System.out.println("Result: PENDING");
                    break;
                }
            }else {
                System.out.println("Result: FAILED");
            }
        }
        return refundList.stream().map(TokenRefundInfoDto::of).collect(Collectors.toList());
    }
}
