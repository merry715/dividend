package com.example.dividend.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * data.go.kr API의 item 필드는 결과가 1건이면 단일 객체,
 * 여러 건이면 배열로 응답함. 두 경우 모두 List로 변환.
 */
public class ItemListDeserializer extends StdDeserializer<List<DataGoKrDividendResponse.Item>> {

    public ItemListDeserializer() {
        super(List.class);
    }

    @Override
    public List<DataGoKrDividendResponse.Item> deserialize(
            JsonParser p, DeserializationContext ctx) throws IOException {

        List<DataGoKrDividendResponse.Item> list = new ArrayList<>();

        if (p.currentToken() == JsonToken.START_ARRAY) {
            // 배열인 경우
            while (p.nextToken() != JsonToken.END_ARRAY) {
                list.add(ctx.readValue(p, DataGoKrDividendResponse.Item.class));
            }
        } else if (p.currentToken() == JsonToken.START_OBJECT) {
            // 단일 객체인 경우
            list.add(ctx.readValue(p, DataGoKrDividendResponse.Item.class));
        }

        return list;
    }
}
