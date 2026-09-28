package com.aitms.domain.recommend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.aitms.domain.requirement.AtomicDecomposition;
import com.aitms.domain.requirement.RequirementType;
import com.aitms.domain.testcase.TestTechnique;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OpenAI Chat Completions API 호출(구조화 출력 response_format=json_schema, strict 모드).
 * SDK 없이 JDK 내장 HttpClient + 기존 Jackson으로 직접 호출(새 의존성 추가 없음).
 * 인증: app.ai.llm.openai-api-key 또는 OPENAI_API_KEY 환경변수.
 * app.ai.llm.provider=openai일 때만 빈으로 등록됨(기본은 {@link AnthropicLlmClient}).
 *
 * {@link LlmProposal}(TC 추천)·{@link AtomicDecomposition}(원자 요구사항 분해) 두 응답 타입만 지원 —
 * 스키마를 각 클래스 구조에 맞춰 수동으로 만든다(Anthropic SDK처럼 임의 클래스에서 자동 생성해주는 게 없어서).
 * 새 responseType이 필요해지면 이 클래스에 분기 하나·스키마 메서드 하나만 추가하면 됨.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai.llm", name = "provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");

    private final String model;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public OpenAiLlmClient(@Value("${app.ai.llm.openai-model:gpt-4o-mini}") String model,
                           @Value("${app.ai.llm.openai-api-key:}") String apiKey,
                           ObjectMapper objectMapper) {
        this.model = model;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T generate(String system, String user, Class<T> responseType) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        String schemaName;
        ObjectNode schema;
        if (responseType == LlmProposal.class) {
            schemaName = "llm_proposal";
            schema = llmProposalSchema();
        } else if (responseType == AtomicDecomposition.class) {
            schemaName = "atomic_decomposition";
            schema = atomicDecompositionSchema();
        } else {
            throw new UnsupportedOperationException(
                    "OpenAiLlmClient는 이 응답 타입을 지원하지 않습니다: " + responseType.getSimpleName());
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);

        ObjectNode jsonSchema = body.putObject("response_format").put("type", "json_schema").putObject("json_schema");
        jsonSchema.put("name", schemaName);
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("OpenAI API 호출 실패 (HTTP " + response.statusCode() + "): "
                    + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        String finishReason = choice.path("finish_reason").asText("");
        if ("length".equals(finishReason)) {
            throw new IllegalStateException("응답이 길이 제한으로 잘렸습니다.");
        }
        if ("content_filter".equals(finishReason)) {
            throw new IllegalStateException("모델이 요청을 거절했습니다.");
        }
        String content = choice.path("message").path("content").asText();
        return (T) objectMapper.readValue(content, responseType);
    }

    /** LlmProposal 구조에 맞춘 JSON 스키마. strict 모드 요구사항: 모든 속성 required + additionalProperties:false. */
    private ObjectNode llmProposalSchema() {
        ObjectNode step = objectMapper.createObjectNode();
        step.put("type", "object");
        ObjectNode stepProps = step.putObject("properties");
        stepProps.putObject("action").put("type", "string");
        stepProps.putObject("expectedResult").put("type", "string");
        step.putArray("required").add("action").add("expectedResult");
        step.put("additionalProperties", false);

        ObjectNode caseNode = objectMapper.createObjectNode();
        caseNode.put("type", "object");
        ObjectNode caseProps = caseNode.putObject("properties");
        caseProps.putObject("atomicRequirementId").put("type", "integer");
        caseProps.putObject("title").put("type", "string");
        ObjectNode technique = caseProps.putObject("technique");
        technique.put("type", "string");
        ArrayNode techniqueEnum = technique.putArray("enum");
        Arrays.stream(TestTechnique.values()).map(Enum::name).forEach(techniqueEnum::add);
        ObjectNode stepsArray = caseProps.putObject("steps");
        stepsArray.put("type", "array");
        stepsArray.set("items", step);
        caseNode.putArray("required").add("atomicRequirementId").add("title").add("technique").add("steps");
        caseNode.put("additionalProperties", false);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode rootProps = root.putObject("properties");
        ObjectNode cases = rootProps.putObject("cases");
        cases.put("type", "array");
        cases.set("items", caseNode);
        root.putArray("required").add("cases");
        root.put("additionalProperties", false);
        return root;
    }

    /** AtomicDecomposition 구조에 맞춘 JSON 스키마. min/max/unit/conditions는 값이 없을 수 있어 nullable로 선언. */
    private ObjectNode atomicDecompositionSchema() {
        ObjectNode atomic = objectMapper.createObjectNode();
        atomic.put("type", "object");
        ObjectNode props = atomic.putObject("properties");
        props.putObject("atomicText").put("type", "string");
        ObjectNode type = props.putObject("type");
        type.put("type", "string");
        ArrayNode typeEnum = type.putArray("enum");
        Arrays.stream(RequirementType.values()).map(Enum::name).forEach(typeEnum::add);
        nullableType(props.putObject("minValue"), "number");
        nullableType(props.putObject("maxValue"), "number");
        nullableType(props.putObject("unit"), "string");
        nullableType(props.putObject("conditions"), "string");
        atomic.putArray("required").add("atomicText").add("type").add("minValue").add("maxValue").add("unit").add("conditions");
        atomic.put("additionalProperties", false);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode rootProps = root.putObject("properties");
        ObjectNode atomics = rootProps.putObject("atomics");
        atomics.put("type", "array");
        atomics.set("items", atomic);
        root.putArray("required").add("atomics");
        root.put("additionalProperties", false);
        return root;
    }

    /** OpenAI strict 모드에서 null 허용 필드는 type을 ["원래타입","null"] 배열로 선언해야 함. */
    private static void nullableType(ObjectNode field, String type) {
        field.putArray("type").add(type).add("null");
    }

    private static String truncate(String s) {
        return s != null && s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }
}
