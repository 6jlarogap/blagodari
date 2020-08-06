package blagodarie.rating.ui.operations;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.PositionalDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import blagodarie.rating.OperationType;
import blagodarie.rating.server.ServerApiResponse;
import blagodarie.rating.server.ServerConnector;

public class OperationDataSource
        extends PositionalDataSource<Operation> {

    private static final String TAG = OperationDataSource.class.getSimpleName();

    @NonNull
    private final UUID mUserId;

    OperationDataSource (@NonNull final UUID userId) {
        Log.d(TAG, "OperationDataSource");
        mUserId = userId;
    }

    @Override
    public void loadInitial (@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Operation> callback) {
        Log.d(TAG, "loadInitial from=" + params.requestedStartPosition + ", pageSize=" + params.pageSize);
        final String content = String.format(Locale.ENGLISH, "{\"uuid\":\"%s\",\"from\":%d,\"count\":%d}", mUserId.toString(), params.requestedStartPosition, params.pageSize);
        try {
            final ServerApiResponse serverApiResponse = ServerConnector.sendRequestAndGetResponse("/getuseroperations", content);
            if (serverApiResponse.getCode() == 200) {
                if (serverApiResponse.getBody() != null) {
                    final String responseBody = serverApiResponse.getBody();
                    Log.d(TAG, "responseBody=" + responseBody);
                    try {
                        final JSONArray jsonOperations = new JSONObject(responseBody).getJSONArray("operations");
                        final List<Operation> operations = new ArrayList<>();
                        for (int i = 0; i < jsonOperations.length(); i++) {
                            final JSONObject operationJsonObject = jsonOperations.getJSONObject(i);
                            final UUID userIdFrom = UUID.fromString(operationJsonObject.getString("user_id_from"));
                            final int operationTypeId = operationJsonObject.getInt("operation_type_id");
                            final Date timestamp = new Date(operationJsonObject.getLong("timestamp"));
                            String comment = operationJsonObject.getString("comment");
                            if (comment.equals("null")) {
                                comment = null;
                            }
                            String photo = operationJsonObject.getString("photo");
                            if (photo.equals("null")) {
                                photo = null;
                            }
                            final String lastName = operationJsonObject.getString("last_name");
                            final String firstName = operationJsonObject.getString("first_name");
                            final OperationType operationType = OperationType.getById(operationTypeId);
                            if (operationType != null) {
                                operations.add(new Operation(userIdFrom, mUserId, photo, lastName, firstName, operationType, comment, timestamp));
                            }
                        }
                        callback.onResult(operations, 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadRange (@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Operation> callback) {
        Log.d(TAG, "loadRange startPosition=" + params.startPosition + ", loadSize=" + params.loadSize);
        final String content = String.format(Locale.ENGLISH, "{\"uuid\":\"%s\",\"from\":%d,\"count\":%d}", mUserId.toString(), params.startPosition, params.loadSize);
        try {
            final ServerApiResponse serverApiResponse = ServerConnector.sendRequestAndGetResponse("/getuseroperations", content);
            if (serverApiResponse.getCode() == 200) {
                if (serverApiResponse.getBody() != null) {
                    final String responseBody = serverApiResponse.getBody();
                    Log.d(TAG, "responseBody=" + responseBody);
                    try {
                        final JSONArray jsonOperations = new JSONObject(responseBody).getJSONArray("operations");
                        final List<Operation> operations = new ArrayList<>();
                        for (int i = 0; i < jsonOperations.length(); i++) {
                            final JSONObject operationJsonObject = jsonOperations.getJSONObject(i);
                            final UUID userIdFrom = UUID.fromString(operationJsonObject.getString("user_id_from"));
                            final int operationTypeId = operationJsonObject.getInt("operation_type_id");
                            final Date timestamp = new Date(operationJsonObject.getLong("timestamp"));
                            String comment = operationJsonObject.getString("comment");
                            if (comment.equals("null")) {
                                comment = null;
                            }
                            String photo = operationJsonObject.getString("photo");
                            if (photo.equals("null")) {
                                photo = null;
                            }
                            final String lastName = operationJsonObject.getString("last_name");
                            final String firstName = operationJsonObject.getString("first_name");
                            final OperationType operationType = OperationType.getById(operationTypeId);
                            if (operationType != null) {
                                operations.add(new Operation(userIdFrom, mUserId, photo, lastName, firstName, operationType, comment, timestamp));
                            }
                        }
                        callback.onResult(operations);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class OperationSourceFactory
            extends Factory<Integer, Operation> {

        @NonNull
        private final UUID mUserId;

        OperationSourceFactory (@NonNull final UUID userId) {
            mUserId = userId;
        }

        @Override
        public DataSource<Integer, Operation> create () {
            return new OperationDataSource(mUserId);
        }

    }
}