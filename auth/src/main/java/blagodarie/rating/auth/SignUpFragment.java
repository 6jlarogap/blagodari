package blagodarie.rating.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.UUID;

import blagodarie.rating.server.ServerApiClient;
import blagodarie.rating.server.SignUpRequest;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

public final class SignUpFragment
        extends Fragment {

    private static final String TAG = SignUpFragment.class.getSimpleName();

    private static final int ACTIVITY_REQUEST_CODE_GOGGLE_SIGN_IN = 1;

    private CompositeDisposable mDisposables = new CompositeDisposable();

    @Override
    public View onCreateView (
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState
    ) {
        Log.d(TAG, "onCreateView");
        final View view = inflater.inflate(R.layout.sign_up_fragment, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onActivityCreated (@Nullable final Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    private void initViews (final View view) {
        Log.d(TAG, "initViews");
        view.findViewById(R.id.btnSignIn).setOnClickListener(
                v -> AuthenticationActivity.googleSignIn(
                        requireActivity(),
                        this,
                        getString(R.string.oauth2_client_id)
                )
        );
    }

    @Override
    public void onDestroy () {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mDisposables.dispose();
    }

    @Override
    public void onActivityResult (
            final int requestCode,
            final int resultCode,
            @Nullable final Intent data
    ) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_GOGGLE_SIGN_IN) {
            final Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                final GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null &&
                        account.getIdToken() != null) {
                    startSignUp(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void startSignUp (
            @NonNull final String googleTokenId
    ) {
        Log.d(TAG, "startSignUp");
        final ServerApiClient apiClient = new ServerApiClient();
        final SignUpRequest signUpRequest = new SignUpRequest(googleTokenId);
        mDisposables.add(
                Observable.
                        fromCallable(() -> apiClient.execute(signUpRequest)).
                        subscribeOn(Schedulers.io()).
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribe(
                                signUpResponse -> createAccount(signUpResponse.getUserId(), signUpResponse.getFirstName(), signUpResponse.getMiddleName(), signUpResponse.getLastName(), signUpResponse.getPhoto(), signUpResponse.getAuthToken()),
                                throwable -> Toast.makeText(requireActivity(), throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    private void createAccount (
            @NonNull final UUID userId,
            @NonNull final String firstName,
            @NonNull final String middleName,
            @NonNull final String lastName,
            @NonNull final String photo,
            @NonNull final String authToken
    ) {
        Log.d(TAG, "createAccount");
        final String accountName = userId.toString();
        final AccountManager accountManager = AccountManager.get(getContext());
        final Account account = new Account(accountName, getString(R.string.account_type));
        final Bundle userData = new Bundle();
        userData.putString(AccountGeneral.USER_DATA_USER_ID, userId.toString());
        userData.putString(AccountGeneral.USER_DATA_FIRST_NAME, firstName);
        userData.putString(AccountGeneral.USER_DATA_MIDDLE_NAME, middleName);
        userData.putString(AccountGeneral.USER_DATA_LAST_NAME, lastName);
        userData.putString(AccountGeneral.USER_DATA_PHOTO, photo);
        accountManager.addAccountExplicitly(account, "", userData);
        accountManager.setAuthToken(account, getString(R.string.token_type), authToken);

        final Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);

        final Intent res = new Intent();
        res.putExtras(bundle);

        ((AuthenticationActivity) requireActivity()).setAccountAuthenticatorResult(bundle);
        requireActivity().setResult(RESULT_OK, res);
        requireActivity().finish();
    }

}
