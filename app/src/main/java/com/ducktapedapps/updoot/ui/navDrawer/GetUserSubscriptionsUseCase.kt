package com.ducktapedapps.updoot.ui.navDrawer

import android.util.Log
import com.ducktapedapps.updoot.data.local.SubredditDAO
import com.ducktapedapps.updoot.data.local.model.LocalSubreddit
import com.ducktapedapps.updoot.utils.accountManagement.AccountModel.AnonymousAccount
import com.ducktapedapps.updoot.utils.accountManagement.AccountModel.UserModel
import com.ducktapedapps.updoot.utils.accountManagement.UpdootAccountsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface GetUserSubscriptionsUseCase {

    val subscriptions: Flow<List<LocalSubreddit>>

}

class GetUserSubscriptionsUseCaseImpl @Inject constructor(
    updootAccountsProvider: UpdootAccountsProvider,
    private val subredditDAO: SubredditDAO,
    private val updateUserSubscriptionUseCase: UpdateUserSubscriptionUseCase,
) : GetUserSubscriptionsUseCase {

    override val subscriptions: Flow<List<LocalSubreddit>> = updootAccountsProvider
        .getCurrentAccount()
        .flatMapLatest { currentAccount ->
            when (currentAccount) {
                is AnonymousAccount -> flowOf(emptyList())
                is UserModel -> subredditDAO
                    .observeSubscribedSubredditsFor(currentAccount.name)
                    .distinctUntilChanged()
                    .onEach {
                        //TODO fix this so that cached subscriptions don't get blocked while syncing new subscriptions
                        if (it.isStale()) updateUserSubscriptionUseCase.updateUserSubscription(
                            currentAccount.name
                        )
                    }
            }
        }.flowOn(Dispatchers.IO)

    private fun List<LocalSubreddit>.isStale(): Boolean = any {
        val diff = System.currentTimeMillis() - it.lastUpdated.time
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diff)
        Log.i("GetUserSubscription", "diff : $diffInHours")
        diffInHours > 1
    }
}