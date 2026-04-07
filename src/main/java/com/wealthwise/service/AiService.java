package com.wealthwise.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.wealthwise.dto.response.MessageDto;
import com.wealthwise.dto.response.PortfolioSummaryResponse;
import com.wealthwise.entity.ChatMessage;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.Holding;
import com.wealthwise.entity.Sip;
import com.wealthwise.entity.Transaction;
import com.wealthwise.entity.User;
import com.wealthwise.repository.ChatMessageRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.HoldingRepository;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final OpenAIClient openAIClient;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final SipRepository sipRepository;
    private final TransactionRepository transactionRepository;
    private final FundRepository fundRepository;
    private final PortfolioService portfolioService;

    @Value("${openai.model}")
    private String model;

    @Transactional
    public String chat(UUID userId, String message) {
        User user = findUser(userId);

        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .role("user")
                .content(message)
                .build();
        chatMessageRepository.save(userMessage);

        try {
            List<Holding> holdings = holdingRepository.findByUserId(userId);
            List<Sip> sips = sipRepository.findByUserId(userId);
            List<Transaction> transactions = transactionRepository.findByUserIdOrderByDateDesc(userId)
                    .stream().limit(5).toList();
            PortfolioSummaryResponse summary = portfolioService.getSummary(userId);
            List<Fund> funds = fundRepository.findAll();
            List<ChatMessage> history = chatMessageRepository.findTop20ByUserIdOrderByCreatedAtAsc(userId);

            String systemPrompt = buildSystemPrompt(user, holdings, sips, transactions, summary, funds);

            ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .maxCompletionTokens(1024)
                    .addSystemMessage(systemPrompt);

            for (ChatMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    paramsBuilder.addUserMessage(msg.getContent());
                } else if ("assistant".equals(msg.getRole())) {
                    paramsBuilder.addMessage(ChatCompletionAssistantMessageParam.builder()
                            .content(msg.getContent())
                            .build());
                }
            }

            paramsBuilder.addUserMessage(message);

            ChatCompletion completion = openAIClient.chat().completions().create(paramsBuilder.build());
            String responseText = completion.choices().get(0).message().content().orElse("");

            ChatMessage assistantMessage = ChatMessage.builder()
                    .user(user)
                    .role("assistant")
                    .content(responseText)
                    .build();
            chatMessageRepository.save(assistantMessage);

            return responseText;
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            chatMessageRepository.delete(userMessage);
            throw new RuntimeException("AI service unavailable");
        }
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getHistory(UUID userId) {
        findUser(userId);
        return chatMessageRepository.findTop20ByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(msg -> MessageDto.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .build())
                .toList();
    }

    @Transactional
    public void clearHistory(UUID userId) {
        findUser(userId);
        chatMessageRepository.deleteByUserId(userId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String buildSystemPrompt(User user, List<Holding> holdings, List<Sip> sips,
                                     List<Transaction> transactions, PortfolioSummaryResponse summary,
                                     List<Fund> funds) {
        StringBuilder sb = new StringBuilder();

        // Identity
        sb.append("You are X, a personal financial co-pilot for Indian retail investors on WealthWise.\n\n");

        // Behaviour guidelines
        sb.append("## Behaviour Guidelines\n");
        sb.append("- Be conversational and friendly.\n");
        sb.append("- Use Indian financial context: amounts in ₹, lakh (L), crore (Cr).\n");
        sb.append("- Only recommend funds from the Available Funds section below.\n");
        sb.append("- Use actual 5Y returns from fund data for SIP return calculations.\n");
        sb.append("- Show moderate and aggressive scenarios when projecting returns.\n");
        sb.append("- Never guarantee returns.\n");
        sb.append("- End every response with a follow-up question.\n");
        sb.append("- Add a disclaimer only on the first message of a conversation.\n\n");

        // User profile
        sb.append("## User Profile\n");
        sb.append("- Name: ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("\n");
        sb.append("- KYC Status: ").append(Boolean.TRUE.equals(user.getKycVerified()) ? "Verified" : "Not Verified").append("\n\n");

        // Portfolio summary
        sb.append("## Portfolio Summary\n");
        if (summary.getTotalInvested().compareTo(BigDecimal.ZERO) == 0) {
            sb.append("No investments yet.\n\n");
        } else {
            sb.append("- Total Invested: ₹").append(formatAmount(summary.getTotalInvested())).append("\n");
            sb.append("- Current Value: ₹").append(formatAmount(summary.getCurrentValue())).append("\n");
            sb.append("- Total Returns: ₹").append(formatAmount(summary.getTotalGain())).append("\n");
            sb.append("- Returns %: ").append(summary.getGainPercent()).append("%\n");
            sb.append("- XIRR: N/A\n\n");
        }

        // Current holdings
        sb.append("## Current Holdings\n");
        if (holdings.isEmpty()) {
            sb.append("None.\n\n");
        } else {
            for (Holding h : holdings) {
                BigDecimal gainPercent = h.getInvested().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : h.getCurValue().subtract(h.getInvested())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(h.getInvested(), 2, RoundingMode.HALF_UP);
                sb.append("- ").append(h.getName())
                        .append(" (").append(h.getCategory()).append(")")
                        .append(" | Invested: ₹").append(formatAmount(h.getInvested()))
                        .append(" | Current: ₹").append(formatAmount(h.getCurValue()))
                        .append(" | Gain: ").append(gainPercent).append("%\n");
            }
            sb.append("\n");
        }

        // Active SIPs
        sb.append("## Active SIPs\n");
        List<Sip> activeSips = sips.stream().filter(s -> "ACTIVE".equals(s.getStatus())).toList();
        if (activeSips.isEmpty()) {
            sb.append("None.\n\n");
        } else {
            for (Sip s : activeSips) {
                sb.append("- ").append(s.getFundName())
                        .append(" | Monthly: ₹").append(formatAmount(s.getMonthlyAmt()))
                        .append(" | Status: ").append(s.getStatus())
                        .append(" | Next Debit: ").append(s.getNextDebit()).append("\n");
            }
            sb.append("\n");
        }

        // Recent transactions
        sb.append("## Recent Transactions\n");
        if (transactions.isEmpty()) {
            sb.append("None.\n\n");
        } else {
            for (Transaction t : transactions) {
                sb.append("- ").append(t.getDate())
                        .append(" | ").append(t.getType())
                        .append(" | ₹").append(formatAmount(t.getAmount()))
                        .append(" | ").append(t.getFundName()).append("\n");
            }
            sb.append("\n");
        }

        // Available funds grouped by category
        sb.append("## Available Funds\n");
        Map<String, List<Fund>> fundsByCategory = funds.stream()
                .collect(Collectors.groupingBy(f -> f.getCategory() != null ? f.getCategory() : "Other"));
        for (Map.Entry<String, List<Fund>> entry : fundsByCategory.entrySet()) {
            sb.append("\n### ").append(entry.getKey()).append("\n");
            for (Fund f : entry.getValue()) {
                sb.append("- ").append(f.getName());
                if (f.getRisk() != null) {
                    sb.append(" | Risk: ").append(f.getRisk());
                }
                if (f.getReturns() != null) {
                    Map<String, BigDecimal> r = f.getReturns();
                    sb.append(" | 1Y: ").append(r.getOrDefault("1Y", BigDecimal.ZERO)).append("%");
                    sb.append(" | 3Y: ").append(r.getOrDefault("3Y", BigDecimal.ZERO)).append("%");
                    sb.append(" | 5Y: ").append(r.getOrDefault("5Y", BigDecimal.ZERO)).append("%");
                }
                if (f.getExpenseRatio() != null) {
                    sb.append(" | ER: ").append(f.getExpenseRatio()).append("%");
                }
                if (f.getMinSip() != null) {
                    sb.append(" | Min SIP: ₹").append(f.getMinSip());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal abs = amount.abs();
        String sign = amount.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";
        if (abs.compareTo(BigDecimal.valueOf(10_000_000)) >= 0) {
            return sign + abs.divide(BigDecimal.valueOf(10_000_000), 2, RoundingMode.HALF_UP) + " Cr";
        } else if (abs.compareTo(BigDecimal.valueOf(100_000)) >= 0) {
            return sign + abs.divide(BigDecimal.valueOf(100_000), 2, RoundingMode.HALF_UP) + " L";
        } else {
            return sign + abs.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
    }
}
