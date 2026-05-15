package com.example.kutirakushala.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.example.kutirakushala.models.SellerWithProducts

class CompanyAdapter(
    private var companies: List<SellerWithProducts>,
    private val onCompanyClick: (SellerWithProducts) -> Unit
) : RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_company, parent, false)
        return CompanyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(companies[position])
    }

    override fun getItemCount() = companies.size

    fun updateCompanies(newCompanies: List<SellerWithProducts>) {
        companies = newCompanies
        notifyDataSetChanged()
    }

    inner class CompanyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCompanyLogo: ImageView = itemView.findViewById(R.id.iv_company_logo)
        private val tvCompanyName: TextView = itemView.findViewById(R.id.tv_company_name)
        private val tvCompanyAddress: TextView = itemView.findViewById(R.id.tv_company_address)
        private val tvProductCount: TextView = itemView.findViewById(R.id.tv_product_count)
        private val tvAvailabilityStatus: TextView = itemView.findViewById(R.id.tv_availability_status)

        fun bind(company: SellerWithProducts) {
            tvCompanyName.text = company.businessName
            tvCompanyAddress.text = company.businessAddress
            tvProductCount.text = "${company.products.size} products"
            
            if (company.isAvailable) {
                tvAvailabilityStatus.text = "● Accepting Orders"
                tvAvailabilityStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                tvAvailabilityStatus.text = "● Not Accepting Orders"
                tvAvailabilityStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }
            
            if (company.logoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(company.logoUrl)
                    .placeholder(R.drawable.ic_business_placeholder)
                    .into(ivCompanyLogo)
            }

            itemView.setOnClickListener {
                onCompanyClick(company)
            }
        }
    }
}