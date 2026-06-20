package com.silas.omaster.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.data.local.SubscriptionManager
import com.silas.omaster.model.Subscription
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.subscription.AddSubscriptionDialog
import com.silas.omaster.ui.subscription.EditSubscriptionDialog
import com.silas.omaster.ui.subscription.SubscriptionDetailBottomSheet
import com.silas.omaster.ui.subscription.SubscriptionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSourceManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscriptions by subManager.subscriptionsFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Subscription?>(null) }
    var selectedSubscription by remember { mutableStateOf<Subscription?>(null) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OMasterTopAppBar(
                title = "预设源管理",
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )

            if (subscriptions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.sub_empty), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subscriptions, key = { it.url }) { sub ->
                        SubscriptionItem(
                            sub = sub,
                            onToggle = { subManager.toggleSubscription(sub.url) },
                            onClick = {
                                selectedSubscription = sub
                                showBottomSheet = true
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onBackground,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.sub_add),
                modifier = Modifier.size(32.dp)
            )
        }

        if (showBottomSheet && selectedSubscription != null) {
            val currentSub = selectedSubscription
            if (currentSub != null) {
                SubscriptionDetailBottomSheet(
                    sub = currentSub,
                    onDismiss = { showBottomSheet = false },
                    sheetState = sheetState,
                    onEdit = {
                        showEditDialog = selectedSubscription
                        showBottomSheet = false
                    },
                    onDelete = {
                        subManager.removeSubscription(currentSub.url)
                        showBottomSheet = false
                    }
                )
            }
        }

        if (showAddDialog) {
            AddSubscriptionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { url ->
                    showAddDialog = false
                    subManager.addSubscription(url = url)
                    Toast.makeText(context, "预设源添加成功", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showEditDialog != null) {
            val editSub = showEditDialog
            if (editSub != null) {
                EditSubscriptionDialog(
                    sub = editSub,
                    onDismiss = { showEditDialog = null },
                    onConfirm = { oldUrl, newUrl ->
                        showEditDialog = null
                        subManager.updateSubscriptionUrl(oldUrl, newUrl)
                        Toast.makeText(context, "预设源更新成功", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
