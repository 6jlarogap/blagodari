package blagodarie.rating.ui.wishes

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import blagodarie.rating.AppExecutors
import blagodarie.rating.R
import blagodarie.rating.databinding.EditWishFragmentBinding
import blagodarie.rating.model.IWish
import blagodarie.rating.model.entities.Wish
import blagodarie.rating.repository.AsyncServerRepository
import blagodarie.rating.server.BadAuthorizationTokenException
import blagodarie.rating.ui.AccountProvider
import blagodarie.rating.ui.AccountSource
import blagodarie.rating.ui.hideSoftKeyboard
import blagodarie.rating.ui.showSoftKeyboard
import blagodarie.rating.ui.wishes.EditWishFragment.UserActionListener

class EditWishFragment : Fragment() {

    fun interface UserActionListener {
        fun onSaveClick()
    }

    companion object {
        private val TAG = EditWishFragment::class.java.simpleName
    }

    private lateinit var mBinding: EditWishFragmentBinding

    private lateinit var mWish: IWish

    private val mAsyncRepository = AsyncServerRepository(AppExecutors.getInstance().networkIO(), AppExecutors.getInstance().mainThread())

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        initBinding(inflater, container)
        return mBinding.root
    }

    override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?
    ) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        val args = EditWishFragmentArgs.fromBundle(requireArguments())
        mWish = args.wish
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated")
        super.onActivityCreated(savedInstanceState)
        setupBinding()
        showSoftKeyboard(requireContext(), mBinding.etWishText)
    }

    private fun initBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
    ) {
        Log.d(TAG, "initBinding")
        mBinding = EditWishFragmentBinding.inflate(inflater, container, false)
    }

    private fun setupBinding() {
        Log.d(TAG, "setupBinding")
        mBinding.userActionListener = UserActionListener { checkAndSaveWish() }
        mBinding.etWishText.append(mWish.text)
        mBinding.etWishText.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAndSaveWish()
                handled = true
            }
            handled
        }
    }

    private fun checkAndSaveWish() {
        val wishText = mBinding.etWishText.text.toString().trim()
        if (wishText.isNotBlank()) {
            hideSoftKeyboard(requireActivity())
            saveWish(wishText)
        } else {
            mBinding.etWishText.error = getString(R.string.err_msg_required_to_fill)
        }
    }

    private fun saveWish(
            wishText: String
    ) {
        AccountSource.requireAccount(
                requireActivity(),
        ) { account: Account? ->
            if (account != null) {
                val wish = Wish(mWish.id, mWish.ownerId, wishText, mWish.lastEdit)
                saveWish(wish, account)
            }
        }
    }

    private fun saveWish(
            wish: IWish,
            account: Account
    ) {
        AccountProvider.getAuthToken(
                requireActivity(),
                account
        ) { authToken: String? ->
            if (authToken != null) {
                saveWish(wish, authToken)
            } else {
                Toast.makeText(requireContext(), R.string.info_msg_need_log_in, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveWish(
            wish: IWish,
            authToken: String
    ) {
        mAsyncRepository.setAuthToken(authToken)
        mAsyncRepository.upsertWish(
                wish,
                {
                    Toast.makeText(requireContext(), R.string.info_msg_wish_saved, Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressed()
                }
        ) { throwable: Throwable ->
            if (throwable is BadAuthorizationTokenException) {
                AccountManager.get(requireContext()).invalidateAuthToken(getString(R.string.account_type), authToken)
                saveWish(wish.text)
            } else {
                Log.e(TAG, Log.getStackTraceString(throwable))
                Toast.makeText(requireContext(), throwable.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}